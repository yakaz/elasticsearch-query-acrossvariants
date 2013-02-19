package org.elasticsearch.index.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossVariantsAndQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AcrossVariantsQueryParser implements QueryParser {

    public static final String NAME = "across_variants";
    public static final String[] NAMES = { NAME, "acrossvariants" };

    private final AnalysisService analysisService;
    private final ScriptService scriptService;

    @Inject
    public AcrossVariantsQueryParser(AnalysisService analysisService, ScriptService scriptService) {
        this.analysisService = analysisService;
        this.scriptService = scriptService;
    }

    @Override
    public String[] names() {
        return NAMES;
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        String value = null;
        float boost = 1.0f;
        Analyzer analyzer = analysisService.defaultSearchAnalyzer();
        Map<String,Float> fieldsBoost = new HashMap<String, Float>();
        String lang = null;
        String script = null;
        Map<String, Object> params = Maps.newHashMap();

        XContentParser.Token token;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else {
                if ("fields".equals(currentFieldName)) {
                    if (token == XContentParser.Token.VALUE_STRING) {
                        parseFields(fieldsBoost, parser.text());
                    } else if (token == XContentParser.Token.START_ARRAY) {
                        fieldsBoost = new HashMap<String, Float>();
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            if (token == XContentParser.Token.VALUE_STRING) {
                                parseFields(fieldsBoost, parser.text());
                            } else {
                                throw new QueryParsingException(parseContext.index(), "["+NAME+"] invalid value type in fields array [" + token + "] only \"field^boost\" is supported");
                            }
                        }
                    } else if (token == XContentParser.Token.START_OBJECT) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                            if (token == XContentParser.Token.FIELD_NAME) {
                                currentFieldName = parser.currentName();
                            } else if (token == XContentParser.Token.VALUE_NUMBER) {
                                fieldsBoost.put(currentFieldName, parser.floatValue());
                            } else {
                                throw new QueryParsingException(parseContext.index(), "["+NAME+"] invalid value type in fields map [" + token + "] only numeric is supported");
                            }
                        }
                    }
                } else if ("text".equals(currentFieldName)) {
                    value = parser.text();
                } else if ("value".equals(currentFieldName)) {
                    value = parser.text();
                } else if ("analyzer".equals(currentFieldName)) {
                    analyzer = analysisService.analyzer(parser.text());
                } else if ("lang".equals(currentFieldName)) {
                    lang = parser.text();
                } else if ("script".equals(currentFieldName)) {
                    script = parser.text();
                } else if ("params".equals(currentFieldName)) {
                    parser.nextToken();
                    params = parser.map();
                } else if ("boost".equals(currentFieldName)) {
                    boost = parser.floatValue();
                } else {
                    throw new QueryParsingException(parseContext.index(), "["+NAME+"] query does not support [" + currentFieldName + "]");
                }
            }
        }

        if (value == null) {
            throw new QueryParsingException(parseContext.index(), "No value specified for "+NAME+" query");
        }

        AcrossVariantsAndQuery.QueryProvider queryProvider = null;
        if (script != null) {
            queryProvider = new ScriptQueryProvider(scriptService.executable(lang, script, params));
        }

        Map<String, Float> mappedFieldsBoost = new HashMap<String, Float>();
        for (Map.Entry<String, Float> boostedField : fieldsBoost.entrySet()) {
            String fieldName = boostedField.getKey();
            MapperService.SmartNameFieldMappers smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);
            if (smartNameFieldMappers != null && smartNameFieldMappers.hasMapper()) {
                fieldName = smartNameFieldMappers.mapper().names().indexName();
            }
            mappedFieldsBoost.put(fieldName, boostedField.getValue());
        }

        Query query;
        if (queryProvider != null)
            query = new AcrossVariantsAndQuery(mappedFieldsBoost, analyzer, value, queryProvider);
        else
            query = new AcrossVariantsAndQuery(mappedFieldsBoost, analyzer, value);
        query.setBoost(boost);
        return query;
    }

    private void parseFields(Map<String,Float> map, String fields) {
        String[] split = fields.split(",");
        for (String f : split) {
            int pos = f.indexOf('^');
            if (pos >= 0) {
                map.put(f.substring(0, pos), Float.parseFloat(f.substring(pos + 1)));
            } else {
                map.put(f, 1.0f);
            }
        }
    }

    public static class ScriptQueryProvider implements AcrossVariantsAndQuery.QueryProvider {

        private ExecutableScript script;
        private Map<String, Object> scriptContext;

        public ScriptQueryProvider(ExecutableScript script) {
            this.script = script;
            this.scriptContext = new HashMap<String, Object>();
        }

        @Override
        public Query queryTerm(String field, String text) {
            scriptContext.clear();
            scriptContext.put("field", field);
            scriptContext.put("text", text);
            scriptContext.put("term", new Term(field, text));
            try {
                script.setNextVar("ctx", scriptContext);
                script.run();
                // Unwrap ctx
                scriptContext.putAll((Map<String, Object>) script.unwrap(scriptContext));
            } catch (Exception e) {
                throw new ElasticSearchIllegalArgumentException("failed to execute script", e);
            }

            try {
                return (Query) scriptContext.get("query");
            } catch (ClassCastException e) {
                throw new ElasticSearchIllegalArgumentException("script did not give a " + Query.class.getCanonicalName() + " in ctx.query", e);
            }
        }
    }

}
