package org.elasticsearch.test.integration.acrossvariants;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossVariantsAndFilter;
import org.apache.lucene.search.BooleanClause;
import org.elasticsearch.common.lucene.search.XBooleanFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.queries.FilterClause;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.lucene.search.MatchAllDocsFilter;
import org.apache.lucene.queries.TermFilter;
import org.elasticsearch.test.integration.ReplayAnalyzer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class AcrossVariantsAndFilterVariantsTest {

    protected TermFilter q(String field, String text) {
        return new TermFilter(new Term(field, text));
    }

    @Test
    public void testToString() throws IOException {
        AcrossVariantsAndFilter Filter;

        Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_46), "simple test");
        assertThat(Filter.toString(), equalTo("[field1]:\"simple test\""));

        Filter = new AcrossVariantsAndFilter(Arrays.asList("field1", "field2"), new WhitespaceAnalyzer(Version.LUCENE_46), "test");
        assertThat(Filter.toString(), equalTo("[field1,field2]:\"test\""));
    }

    @Test
    public void testSingle() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_46), "a");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, equalTo((Filter)q("field1", "a")));
    }

    @Test
    public void testSimple() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_46), "a b c");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(XBooleanFilter.class));

        assertThat(((XBooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
                new FilterClause(q("field1", "a"), BooleanClause.Occur.MUST),
                new FilterClause(q("field1", "b"), BooleanClause.Occur.MUST),
                new FilterClause(q("field1", "c"), BooleanClause.Occur.MUST)
        )));
    }

    @Test
    public void testAcross() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1", "field2"), new WhitespaceAnalyzer(Version.LUCENE_46), "a b");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(XBooleanFilter.class));

        XBooleanFilter level0 = new XBooleanFilter();
        XBooleanFilter filterA = new XBooleanFilter();
        XBooleanFilter filterB = new XBooleanFilter();

        level0.add(filterA, BooleanClause.Occur.MUST);
        level0.add(filterB, BooleanClause.Occur.MUST);
        filterA.add(q("field1", "a"), BooleanClause.Occur.SHOULD);
        filterA.add(q("field2", "a"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field1", "b"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field2", "b"), BooleanClause.Occur.SHOULD);

        assertThat((XBooleanFilter) rewritten, equalTo(level0));
    }

    @Test
    public void testFilterProvider() throws IOException {
        AcrossVariantsAndFilter.FilterProvider FilterProvider = new AcrossVariantsAndFilter.FilterProvider() {
            @Override
            public Filter filterTerm(String field, String term) {
                return new MatchAllDocsFilter();
            }
        };

        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_46), "a", FilterProvider);
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(MatchAllDocsFilter.class));
    }

    @Test
    public void testSimpleVariant() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(
                Arrays.asList("field1"),
                new ReplayAnalyzer(
                        new String[]{"a", "A"},
                        new int[]   {  0,   0},
                        new int[]   {  0,   0},
                        new int[]   {  1,   1}
                ),
                "DUMMY a:A");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(XBooleanFilter.class));

        assertThat(((XBooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
                new FilterClause(q("field1", "a"), BooleanClause.Occur.SHOULD),
                new FilterClause(q("field1", "A"), BooleanClause.Occur.SHOULD)
        )));
    }

    @Test
    public void testSimpleNesting() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(
                Arrays.asList("field1"),
                new ReplayAnalyzer(
                        new String[]{"a", "wi-fi", "wi", "fi", "hotspot"},
                        new int[]   {  0,       1,    0,    0,         1},
                        new int[]   {  0,       2,    2,    5,         8},
                        new int[]   {  1,       7,    4,    7,        15}
                ),
                //               111111
                //     0123456789012345
                "DUMMY a wi-fi hotspot");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(XBooleanFilter.class));

        XBooleanFilter level0 = new XBooleanFilter();
        XBooleanFilter level1 = new XBooleanFilter();
        XBooleanFilter level2 = new XBooleanFilter();
        Filter filterA = q("field1", "a");
        Filter filterWiFi = q("field1", "wi-fi");
        Filter filterWi = q("field1", "wi");
        Filter filterFi = q("field1", "fi");
        Filter filterHotspot = q("field1", "hotspot");

        level0.add(filterA, BooleanClause.Occur.MUST);
        level0.add(level1, BooleanClause.Occur.MUST);
        {
            level1.add(filterWiFi, BooleanClause.Occur.SHOULD);
            level1.add(level2, BooleanClause.Occur.SHOULD);
            {
                level2.add(filterWi, BooleanClause.Occur.MUST);
                level2.add(filterFi, BooleanClause.Occur.MUST);
            }
        }
        level0.add(filterHotspot, BooleanClause.Occur.MUST);

        assertThat((XBooleanFilter) rewritten, equalTo(level0));
    }

    @Test
    public void testMultipleNesting() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(
                Arrays.asList("field1"),
                new ReplayAnalyzer(
                        new String[]{"a", "b", "d", "e", "g", "h", "f", "c"},
                        new int[]   {  0,   0,   0,   0,   0,   0,   0,   1},
                        new int[]   {  0,   2,   5,   7,  10,  12,  15,  18},
                        new int[]   {  1,  17,   6,  14,  11,  13,  16,  19}
                ),
                //               1111111111
                //     01234567890123456789
                "DUMMY a b:(d e:(g h) f) c");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(XBooleanFilter.class));

        XBooleanFilter level0 = new XBooleanFilter();
        XBooleanFilter level1 = new XBooleanFilter();
        XBooleanFilter level2 = new XBooleanFilter();
        XBooleanFilter level3 = new XBooleanFilter();
        XBooleanFilter level4 = new XBooleanFilter();
        Filter filterA = q("field1", "a");
        Filter filterB = q("field1", "b");
        Filter filterC = q("field1", "c");
        Filter filterD = q("field1", "d");
        Filter filterE = q("field1", "e");
        Filter filterF = q("field1", "f");
        Filter filterG = q("field1", "g");
        Filter filterH = q("field1", "h");

        level0.add(filterA, BooleanClause.Occur.MUST);
        level0.add(level1, BooleanClause.Occur.MUST);
        {
            level1.add(filterB, BooleanClause.Occur.SHOULD);
            level1.add(level2, BooleanClause.Occur.SHOULD);
            {
                level2.add(filterD, BooleanClause.Occur.MUST);
                level2.add(level3, BooleanClause.Occur.MUST);
                {
                    level3.add(filterE, BooleanClause.Occur.SHOULD);
                    level3.add(level4, BooleanClause.Occur.SHOULD);
                    {
                        level4.add(filterG, BooleanClause.Occur.MUST);
                        level4.add(filterH, BooleanClause.Occur.MUST);
                    }
                }
                level2.add(filterF, BooleanClause.Occur.MUST);
            }
        }
        level0.add(filterC, BooleanClause.Occur.MUST);

        assertThat((XBooleanFilter) rewritten, equalTo(level0));
    }

}
