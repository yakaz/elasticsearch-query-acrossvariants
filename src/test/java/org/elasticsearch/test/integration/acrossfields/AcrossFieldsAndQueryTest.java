package org.elasticsearch.test.integration.acrossfields;

import org.elasticsearch.index.query.AcrossFieldsQueryBuilder;
import org.elasticsearch.test.integration.BaseESTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Test
public class AcrossFieldsAndQueryTest extends BaseESTest {

    @Test
    public void testSimple() throws IOException {
        indexDoc(doc("1", "field1", "one", "field2", "two"));
        indexDoc(doc("2", "field1", "foo", "field2", "two"));
        commit();

        assertDocs(new AcrossFieldsQueryBuilder().field("field1").value("one").analyzer("whitespace"),
                "1");
        assertDocs(new AcrossFieldsQueryBuilder().field("field2").value("two").analyzer("whitespace"),
                "1",
                "2");
    }

    @Test
    public void testAcross() throws IOException {
        indexDoc(doc("1", "field1", "a b c", "field2", "d e f"));
        indexDoc(doc("2", "field1", "a e c", "field2", "d b f"));
        indexDoc(doc("3", "field1", "y", "field2", "z"));
        commit();

        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("b").analyzer("whitespace"),
                "1");
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1", "field2").value("a b c").analyzer("whitespace"),
                "1",
                "2");
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1", "field2").value("d e f").analyzer("whitespace"),
                "1",
                "2");
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1", "field2").value("y z").analyzer("whitespace"),
                "3");
    }

    @Test
    public void testDefaultAnalyzer() throws IOException {
        indexDoc(doc("1", "field1", "stopword index search"));
        commit();

        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("stopword")
                ); // no results as a is a stopword for the default search analyzer
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("search")
                ); // no results as a is a stopword for the default search analyzer
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("index"),
                "1"); // index is a stopword for the default index analyzer, but not the default search analyzer
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("stopword").analyzer("whitespace"),
                "1");
    }

    @Test
    public void testBoosting() throws IOException {
        indexDoc(doc("1", "field1", "a"));
        indexDoc(doc("2", "field2", "a"));
        commit();

        assertDocs(new AcrossFieldsQueryBuilder().fields("field1^2", "field2^0.5").value("a").analyzer("whitespace"),
                "1",
                "2");
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1^0.5", "field2^2").value("a").analyzer("whitespace"),
                "2",
                "1");
    }

    @Test
    public void testQueryProvider() throws IOException {
        indexDoc(doc("1", "field1", "a"));
        indexDoc(doc("2", "field1", "aa"));
        commit();

        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("delta", Integer.valueOf(1));
        assertDocs(new AcrossFieldsQueryBuilder().fields("field1").value("a").analyzer("whitespace")
                .lang("mvel").params(params).script(
                        "ctx.text = ctx.text + ctx.text;" +
                        "ctx.query = new org.apache.lucene.search.TermQuery(new org.apache.lucene.index.Term(ctx.field, ctx.text));"
                ),
                "2");
    }

}
