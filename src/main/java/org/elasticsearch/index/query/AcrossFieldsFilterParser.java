package org.elasticsearch.index.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AcrossFieldsAndFilter;
import org.apache.lucene.search.Filter;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.cache.filter.support.CacheKeyFilter;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AcrossFieldsFilterParser implements FilterParser {

    public static final String NAME = "across_fields";
    public static final String[] NAMES = { NAME, "acrossfields" };

    private final AnalysisService analysisService;
    private final ScriptService scriptService;

    @Inject
    public AcrossFieldsFilterParser(AnalysisService analysisService, ScriptService scriptService) {
        this.analysisService = analysisService;
        this.scriptService = scriptService;
    }

    @Override
    public String[] names() {
        return NAMES;
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        boolean cache = false;
        CacheKeyFilter.Key cacheKey = null;
        String value = null;
        Analyzer analyzer = analysisService.defaultSearchAnalyzer();
        Collection<String> fields = new ArrayList<String>();
        String lang = null;
        String script = null;
        Map<String, Object> params = Maps.newHashMap();

        XContentParser.Token token;
        String filterName = null;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else {
                if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else if ("_cache".equals(currentFieldName)) {
                    cache = parser.booleanValue();
                } else if ("_cache_key".equals(currentFieldName) || "_cacheKey".equals(currentFieldName)) {
                    cacheKey = new CacheKeyFilter.Key(parser.text());
                } else if ("fields".equals(currentFieldName)) {
                    if (token == XContentParser.Token.VALUE_STRING) {
                        fields = Arrays.asList(parser.text().split(","));
                    } else if (token == XContentParser.Token.START_ARRAY) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            if (token == XContentParser.Token.VALUE_STRING) {
                                fields.add(parser.text());
                            } else {
                                throw new QueryParsingException(parseContext.index(), "["+NAME+"] invalid value type in fields array [" + token + "] only \"field^boost\" is supported");
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
                } else {
                    throw new QueryParsingException(parseContext.index(), "["+NAME+"] query does not support [" + currentFieldName + "]");
                }
            }
        }

        if (value == null) {
            throw new QueryParsingException(parseContext.index(), "No value specified for "+NAME+" query");
        }

        AcrossFieldsAndFilter.FilterProvider filterProvider = null;
        if (script != null) {
            filterProvider = new ScriptFilterProvider(scriptService.executable(lang, script, params));
        }


        Collection<String> mappedFields = new ArrayList<String>(fields.size());
        for (String fieldName : fields) {
            MapperService.SmartNameFieldMappers smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);
            if (smartNameFieldMappers != null && smartNameFieldMappers.hasMapper()) {
                fieldName = smartNameFieldMappers.mapper().names().indexName();
            }
            mappedFields.add(fieldName);
        }

        Filter filter = null;
        if (filterProvider != null)
            filter = new AcrossFieldsAndFilter(mappedFields, analyzer, value, filterProvider);
        else
            filter = new AcrossFieldsAndFilter(mappedFields, analyzer, value);

        if (cache) {
            filter = parseContext.cacheFilter(filter, cacheKey);
        }

        if (filterName != null) {
            parseContext.addNamedFilter(filterName, filter);
        }
        return filter;
    }

    public static class ScriptFilterProvider implements AcrossFieldsAndFilter.FilterProvider {

        private ExecutableScript script;
        private Map<String, Object> scriptContext;

        public ScriptFilterProvider(ExecutableScript script) {
            this.script = script;
            this.scriptContext = new HashMap<String, Object>();
        }

        @Override
        public Filter filterTerm(String field, String text) {
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
                return (Filter) scriptContext.get("filter");
            } catch (ClassCastException e) {
                throw new ElasticSearchIllegalArgumentException("script did not give a " + Filter.class.getCanonicalName() + " in ctx.filter", e);
            }
        }
    }

}
