package org.elasticsearch.index.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.AcrossFieldsAndQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.mapper.MapperService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AcrossFieldsQueryParser implements QueryParser {

    public static final String NAME = "acrossfields";

    private final AnalysisService analysisService;

    @Inject
    public AcrossFieldsQueryParser(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        String value = null;
        float boost = 1.0f;
        Analyzer analyzer = analysisService.defaultSearchAnalyzer();
        Map<String,Float> fieldsBoost = new HashMap<String, Float>();

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

        Map<String, Float> mappedFieldsBoost = new HashMap<String, Float>();
        for (Map.Entry<String, Float> boostedField : fieldsBoost.entrySet()) {
            String fieldName = boostedField.getKey();
            MapperService.SmartNameFieldMappers smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);
            if (smartNameFieldMappers != null && smartNameFieldMappers.hasMapper()) {
                fieldName = smartNameFieldMappers.mapper().names().indexName();
            }
            mappedFieldsBoost.put(fieldName, boostedField.getValue());
        }

        Query query = new AcrossFieldsAndQuery(mappedFieldsBoost, analyzer, value);
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

}
