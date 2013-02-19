package org.elasticsearch.test.integration.acrossvariants;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossVariantsAndQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.elasticsearch.test.integration.ReplayAnalyzer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class AcrossVariantsAndQueryVariantsTest {

    protected TermQuery q(String field, String text) {
        return q(field, text, 1.0f);
    }
    protected TermQuery q(String field, String text, float boost) {
        TermQuery rtn = new TermQuery(new Term(field, text));
        rtn.setBoost(boost);
        return rtn;
    }

    @Test
    public void testToString() throws IOException {
        AcrossVariantsAndQuery query;

        query = new AcrossVariantsAndQuery(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "simple test");
        assertThat(query.toString(), equalTo("[field1]:\"simple test\""));

        query = new AcrossVariantsAndQuery(Arrays.asList("field1", "field2"), new WhitespaceAnalyzer(Version.LUCENE_35), "test");
        assertThat(query.toString(), equalTo("[field1,field2]:\"test\""));

        Map<String, Float> boostedFields = new HashMap<String, Float>();
        boostedFields.put("field1", 1.0f);
        boostedFields.put("field2", 2.0f);
        query = new AcrossVariantsAndQuery(boostedFields, new WhitespaceAnalyzer(Version.LUCENE_35), "simple test");
        assertThat(query.toString(), equalTo("[field2^"+2.0f+",field1]:\"simple test\""));
    }

    @Test
    public void testSingle() throws IOException {
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a");
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(TermQuery.class));

        assertThat((TermQuery) rewritten, equalTo(q("field1", "a")));
    }

    @Test
    public void testSimple() throws IOException {
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a b c");
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        assertThat(((BooleanQuery) rewritten).clauses(), equalTo(Arrays.asList(
                new BooleanClause(q("field1", "a"), BooleanClause.Occur.MUST),
                new BooleanClause(q("field1", "b"), BooleanClause.Occur.MUST),
                new BooleanClause(q("field1", "c"), BooleanClause.Occur.MUST)
        )));
    }

    @Test
    public void testAcross() throws IOException {
        Map<String, Float> boostedFields = new HashMap<String, Float>(2);
        boostedFields.put("field1", 0.5f);
        boostedFields.put("field2", 2.0f);
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(boostedFields, new WhitespaceAnalyzer(Version.LUCENE_35), "a b");
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        BooleanQuery level0 = new BooleanQuery(true);
        BooleanQuery queryA = new BooleanQuery(true);
        BooleanQuery queryB = new BooleanQuery(true);

        level0.add(queryA, BooleanClause.Occur.MUST);
        level0.add(queryB, BooleanClause.Occur.MUST);
        queryA.add(q("field1", "a", 0.5f), BooleanClause.Occur.SHOULD);
        queryA.add(q("field2", "a", 2.0f), BooleanClause.Occur.SHOULD);
        queryB.add(q("field1", "b", 0.5f), BooleanClause.Occur.SHOULD);
        queryB.add(q("field2", "b", 2.0f), BooleanClause.Occur.SHOULD);

        assertThat((BooleanQuery) rewritten, equalTo(level0));
    }

    @Test
    public void testQueryProvider() throws IOException {
        AcrossVariantsAndQuery.QueryProvider queryProvider = new AcrossVariantsAndQuery.QueryProvider() {
            @Override
            public Query queryTerm(String field, String term) {
                return new MatchAllDocsQuery();
            }
        };

        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(Arrays.asList("field1"), new WhitespaceAnalyzer(Version.LUCENE_35), "a", queryProvider);
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(MatchAllDocsQuery.class));
    }

    @Test
    public void testSimpleVariant() throws IOException {
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(
                Arrays.asList("field1"),
                new ReplayAnalyzer(
                        new String[]{"a", "A"},
                        new int[]   {  0,   0},
                        new int[]   {  0,   0},
                        new int[]   {  1,   1}
                ),
                "DUMMY a:A");
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        assertThat(((BooleanQuery) rewritten).clauses(), equalTo(Arrays.asList(
                new BooleanClause(q("field1", "a"), BooleanClause.Occur.SHOULD),
                new BooleanClause(q("field1", "A"), BooleanClause.Occur.SHOULD)
        )));
    }

    @Test
    public void testSimpleNesting() throws IOException {
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(
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
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        BooleanQuery level0 = new BooleanQuery(true);
        BooleanQuery level1 = new BooleanQuery(true);
        BooleanQuery level2 = new BooleanQuery(true);

        level0.add(q("field1", "a"), BooleanClause.Occur.MUST);
        level0.add(level1, BooleanClause.Occur.MUST);
        {
            level1.add(q("field1", "wi-fi"), BooleanClause.Occur.SHOULD);
            level1.add(level2, BooleanClause.Occur.SHOULD);
            {
                level2.add(q("field1", "wi"), BooleanClause.Occur.MUST);
                level2.add(q("field1", "fi"), BooleanClause.Occur.MUST);
            }
        }
        level0.add(q("field1", "hotspot"), BooleanClause.Occur.MUST);

        assertThat((BooleanQuery) rewritten, equalTo(level0));
    }

    @Test
    public void testMultipleNesting() throws IOException {
        AcrossVariantsAndQuery query = new AcrossVariantsAndQuery(
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
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        BooleanQuery level0 = new BooleanQuery(true);
        BooleanQuery level1 = new BooleanQuery(true);
        BooleanQuery level2 = new BooleanQuery(true);
        BooleanQuery level3 = new BooleanQuery(true);
        BooleanQuery level4 = new BooleanQuery(true);

        level0.add(q("field1", "a"), BooleanClause.Occur.MUST);
        level0.add(level1, BooleanClause.Occur.MUST);
        {
            level1.add(q("field1", "b"), BooleanClause.Occur.SHOULD);
            level1.add(level2, BooleanClause.Occur.SHOULD);
            {
                level2.add(q("field1", "d"), BooleanClause.Occur.MUST);
                level2.add(level3, BooleanClause.Occur.MUST);
                {
                    level3.add(q("field1", "e"), BooleanClause.Occur.SHOULD);
                    level3.add(level4, BooleanClause.Occur.SHOULD);
                    {
                        level4.add(q("field1", "g"), BooleanClause.Occur.MUST);
                        level4.add(q("field1", "h"), BooleanClause.Occur.MUST);
                    }
                }
                level2.add(q("field1", "f"), BooleanClause.Occur.MUST);
            }
        }
        level0.add(q("field1", "c"), BooleanClause.Occur.MUST);

        assertThat((BooleanQuery) rewritten, equalTo(level0));
    }

}
