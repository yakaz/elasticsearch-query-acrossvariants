package org.elasticsearch.test.integration.acrossvariants;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossVariantsAndFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.lucene.search.MatchAllDocsFilter;
import org.elasticsearch.common.lucene.search.TermFilter;
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

        Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "simple test");
        assertThat(Filter.toString(), equalTo("[field1]:\"simple test\""));

        Filter = new AcrossVariantsAndFilter(Arrays.asList("field1", "field2"), new WhitespaceAnalyzer(Version.LUCENE_35), "test");
        assertThat(Filter.toString(), equalTo("[field1,field2]:\"test\""));
    }

    @Test
    public void testSingle() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        assertThat(((BooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
                new FilterClause(q("field1", "a"), BooleanClause.Occur.SHOULD))));
    }

    @Test
    public void testSimple() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a b c");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        BooleanFilter filterA = new BooleanFilter();
        BooleanFilter filterB = new BooleanFilter();
        BooleanFilter filterC = new BooleanFilter();
        filterA.add(q("field1", "a"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field1", "b"), BooleanClause.Occur.SHOULD);
        filterC.add(q("field1", "c"), BooleanClause.Occur.SHOULD);

        assertThat(((BooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
                new FilterClause(filterA, BooleanClause.Occur.MUST),
                new FilterClause(filterB, BooleanClause.Occur.MUST),
                new FilterClause(filterC, BooleanClause.Occur.MUST)
        )));
    }

    @Test
    public void testAcross() throws IOException {
        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1", "field2"), new WhitespaceAnalyzer(Version.LUCENE_35), "a b");
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        BooleanFilter level0 = new BooleanFilter();
        BooleanFilter filterA = new BooleanFilter();
        BooleanFilter filterB = new BooleanFilter();

        level0.add(filterA, BooleanClause.Occur.MUST);
        level0.add(filterB, BooleanClause.Occur.MUST);
        filterA.add(q("field1", "a"), BooleanClause.Occur.SHOULD);
        filterA.add(q("field2", "a"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field1", "b"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field2", "b"), BooleanClause.Occur.SHOULD);

        assertThat((BooleanFilter) rewritten, equalTo(level0));
    }

    @Test
    public void testFilterProvider() throws IOException {
        AcrossVariantsAndFilter.FilterProvider FilterProvider = new AcrossVariantsAndFilter.FilterProvider() {
            @Override
            public Filter filterTerm(String field, String term) {
                return new MatchAllDocsFilter();
            }
        };

        AcrossVariantsAndFilter Filter = new AcrossVariantsAndFilter(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a", FilterProvider);
        Filter rewritten = Filter.rewrite();
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        assertThat(((BooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
                new FilterClause(new MatchAllDocsFilter(), BooleanClause.Occur.SHOULD)
        )));
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
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        assertThat(((BooleanFilter) rewritten).clauses(), equalTo(Arrays.asList(
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
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        BooleanFilter level0 = new BooleanFilter();
        BooleanFilter level1 = new BooleanFilter();
        BooleanFilter level2 = new BooleanFilter();
        BooleanFilter filterA = new BooleanFilter();
        BooleanFilter filterWiFi = new BooleanFilter();
        BooleanFilter filterWi = new BooleanFilter();
        BooleanFilter filterFi = new BooleanFilter();
        BooleanFilter filterHotspot = new BooleanFilter();

        filterA.add(q("field1", "a"), BooleanClause.Occur.SHOULD);
        filterWiFi.add(q("field1", "wi-fi"), BooleanClause.Occur.SHOULD);
        filterWi.add(q("field1", "wi"), BooleanClause.Occur.SHOULD);
        filterFi.add(q("field1", "fi"), BooleanClause.Occur.SHOULD);
        filterHotspot.add(q("field1", "hotspot"), BooleanClause.Occur.SHOULD);

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

        assertThat((BooleanFilter) rewritten, equalTo(level0));
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
        assertThat(rewritten, instanceOf(BooleanFilter.class));

        BooleanFilter level0 = new BooleanFilter();
        BooleanFilter level1 = new BooleanFilter();
        BooleanFilter level2 = new BooleanFilter();
        BooleanFilter level3 = new BooleanFilter();
        BooleanFilter level4 = new BooleanFilter();
        BooleanFilter filterA = new BooleanFilter();
        BooleanFilter filterB = new BooleanFilter();
        BooleanFilter filterC = new BooleanFilter();
        BooleanFilter filterD = new BooleanFilter();
        BooleanFilter filterE = new BooleanFilter();
        BooleanFilter filterF = new BooleanFilter();
        BooleanFilter filterG = new BooleanFilter();
        BooleanFilter filterH = new BooleanFilter();
        filterA.add(q("field1", "a"), BooleanClause.Occur.SHOULD);
        filterB.add(q("field1", "b"), BooleanClause.Occur.SHOULD);
        filterC.add(q("field1", "c"), BooleanClause.Occur.SHOULD);
        filterD.add(q("field1", "d"), BooleanClause.Occur.SHOULD);
        filterE.add(q("field1", "e"), BooleanClause.Occur.SHOULD);
        filterF.add(q("field1", "f"), BooleanClause.Occur.SHOULD);
        filterG.add(q("field1", "g"), BooleanClause.Occur.SHOULD);
        filterH.add(q("field1", "h"), BooleanClause.Occur.SHOULD);

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

        assertThat((BooleanFilter) rewritten, equalTo(level0));
    }

}
