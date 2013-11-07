package org.elasticsearch.test.integration.acrossvariants;

import org.elasticsearch.index.query.AcrossVariantsFilterBuilder;
import org.elasticsearch.index.query.AcrossVariantsQueryBuilder;
import org.elasticsearch.test.integration.BaseESTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Test
public class AcrossVariantsAndFilterTest extends BaseESTest {

    @Test
    public void testSimple() throws IOException {
        indexDoc(doc("1", "field1", "one", "field2", "two"));
        indexDoc(doc("2", "field1", "foo", "field2", "two"));
        commit();

        assertDocs(new AcrossVariantsFilterBuilder().field("field1").value("one").analyzer("whitespace"),
                "1");
        assertDocs(new AcrossVariantsFilterBuilder().field("field2").value("two").analyzer("whitespace"),
                "1",
                "2");
    }

    @Test
    public void testAcross() throws IOException {
        indexDoc(doc("1", "field1", "a b c", "field2", "d e f"));
        indexDoc(doc("2", "field1", "a e c", "field2", "d b f"));
        indexDoc(doc("3", "field1", "y", "field2", "z"));
        commit();

        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("b").analyzer("whitespace"),
                "1");
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1", "field2").value("a b c").analyzer("whitespace"),
                "1",
                "2");
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1", "field2").value("d e f").analyzer("whitespace"),
                "1",
                "2");
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1", "field2").value("y z").analyzer("whitespace"),
                "3");
    }

    @Test
    public void testFields() throws IOException {
        indexDoc(doc("1", "field1", "one", "field2", "two"));
        indexDoc(doc("2", "field1", "two", "field2", "one"));
        commit();

        assertDocs(new AcrossVariantsQueryBuilder().fields("field1,field2").value("one").analyzer("whitespace"),
                "1", "2");

        assertDocs(new AcrossVariantsQueryBuilder().fields(" field1 , field2 ").value("one").analyzer("whitespace"),
                "1", "2");

        Collection<String> list = new ArrayList<String>(2);
        list.add("field1");
        list.add(" field2 ");
        assertDocs(new AcrossVariantsQueryBuilder().fields(list).value("one").analyzer("whitespace"),
                "1", "2");

        Map<String, Integer> imap = new HashMap<String, Integer>(2);
        imap.put("field1", 1);
        imap.put(" field2 ", 1);
        assertDocs(new AcrossVariantsQueryBuilder().fields(imap).value("one").analyzer("whitespace"),
                "1", "2");

        Map<String, Float> fmap = new HashMap<String, Float>(2);
        fmap.put("field1", 1.0f);
        fmap.put(" field2 ", 1.0f);
        assertDocs(new AcrossVariantsQueryBuilder().fields(fmap).value("one").analyzer("whitespace"),
                "1", "2");

        Map<String, Double> dmap = new HashMap<String, Double>(2);
        dmap.put("field1", 1.0);
        dmap.put(" field2 ", 1.0);
        assertDocs(new AcrossVariantsQueryBuilder().fields(dmap).value("one").analyzer("whitespace"),
                "1", "2");
    }

    @Test
    public void testDefaultAnalyzer() throws IOException {
        indexDoc(doc("1", "field1", "stopword index search"));
        commit();

        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("stopword")
                ); // no results as a is a stopword for the default search analyzer
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("search")
                ); // no results as a is a stopword for the default search analyzer
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("index"),
                "1"); // index is a stopword for the default index analyzer, but not the default search analyzer
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("stopword").analyzer("whitespace"),
                "1");
    }

    @Test
    public void testFilterProvider() throws IOException {
        indexDoc(doc("1", "field1", "a"));
        indexDoc(doc("2", "field1", "aa"));
        commit();

        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("delta", Integer.valueOf(1));
        assertDocs(new AcrossVariantsFilterBuilder().fields("field1").value("a").analyzer("whitespace")
                .lang("mvel").params(params).script(
                        "ctx.text = ctx.text + ctx.text;" +
                        "ctx.filter = new org.apache.lucene.queries.TermFilter(new org.apache.lucene.index.Term(ctx.field, ctx.text));"
                ),
                "2");
    }

}
