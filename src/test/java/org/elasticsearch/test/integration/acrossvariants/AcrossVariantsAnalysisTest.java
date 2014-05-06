package org.elasticsearch.test.integration.acrossvariants;

import org.elasticsearch.index.query.AcrossVariantsFilterBuilder;
import org.elasticsearch.index.query.AcrossVariantsQueryBuilder;
import org.elasticsearch.test.integration.BaseESTest;
import org.testng.annotations.Test;

import java.io.IOException;

@Test
public class AcrossVariantsAnalysisTest extends BaseESTest {

    @Test
    public void testPatternTokenizerQuery() throws IOException {
        indexDoc(doc("1", "field1", "a b c"));
        indexDoc(doc("2", "field1", "a-b c"));
        commit();

        assertDocs(new AcrossVariantsQueryBuilder().field("field1").value("a").analyzer("custom_analyzer"),
                "1");
    }

    @Test
    public void testPatternTokenizerFilter() throws IOException {
        indexDoc(doc("1", "field1", "a b c"));
        indexDoc(doc("2", "field1", "a-b c"));
        commit();

        assertDocs(new AcrossVariantsFilterBuilder().field("field1").value("a").analyzer("custom_analyzer"),
                "1");
    }

}
