package org.elasticsearch.test.integration.acrossvariants;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossVariantsAndQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.elasticsearch.test.integration.ReplayAnalyzer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
public class AcrossVariantsAndQueryDisMaxTest {

    protected TermQuery q(String field, String text) {
        return q(field, text, 1.0f);
    }
    protected TermQuery q(String field, String text, float boost) {
        TermQuery rtn = new TermQuery(new Term(field, text));
        rtn.setBoost(boost);
        return rtn;
    }

    @Test
    public void testMultipleNesting() throws IOException {
        float tieBreaker = 2.0f;
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
        query.setUseDisMax(true);
        query.setTieBreaker(tieBreaker);
        Query rewritten = query.rewrite(null);
        assertThat(rewritten, instanceOf(BooleanQuery.class));

        BooleanQuery        level0 = new BooleanQuery(true);
        DisjunctionMaxQuery level1 = new DisjunctionMaxQuery(tieBreaker);
        BooleanQuery        level2 = new BooleanQuery(true);
        DisjunctionMaxQuery level3 = new DisjunctionMaxQuery(tieBreaker);
        BooleanQuery        level4 = new BooleanQuery(true);

        level0.add(q("field1", "a"), BooleanClause.Occur.MUST);
        level0.add(level1, BooleanClause.Occur.MUST);
        {
            level1.add(q("field1", "b"));
            level1.add(level2);
            {
                level2.add(q("field1", "d"), BooleanClause.Occur.MUST);
                level2.add(level3, BooleanClause.Occur.MUST);
                {
                    level3.add(q("field1", "e"));
                    level3.add(level4);
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
