package org.elasticsearch.index.query;

import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AcrossVariantsQueryBuilder extends BaseQueryBuilder {

    private Map<String, Float> boostedFields = new HashMap<String, Float>();
    private String value;
    private String analyzer;
    private String lang;
    private String script;
    Map<String, Object> params;
    private boolean hasBoostedFields = false;
    private float boost = -1;

    public AcrossVariantsQueryBuilder(Map<String, Float> boostedFields, String value, String analyzer) {
        this.boostedFields = boostedFields;
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossVariantsQueryBuilder(String value, String analyzer) {
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossVariantsQueryBuilder(String value) {
        this.value = value;
    }

    public AcrossVariantsQueryBuilder() {
    }

    public AcrossVariantsQueryBuilder clearFields() {
        boostedFields.clear();
        hasBoostedFields = false;
        return this;
    }

    public AcrossVariantsQueryBuilder addField(String field) {
        boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossVariantsQueryBuilder field(String field) {
        return clearFields().addField(field);
    }

    public AcrossVariantsQueryBuilder addField(String field, float boost) {
        boostedFields.put(field, boost);
        hasBoostedFields = true;
        return this;
    }

    public AcrossVariantsQueryBuilder field(String field, float boost) {
        return clearFields().addField(field, boost);
    }

    public AcrossVariantsQueryBuilder addFields(Collection<String> fields) {
        for (String field : fields)
            boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossVariantsQueryBuilder addFields(String... fields) {
        for (String field : fields)
            boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossVariantsQueryBuilder fields(Collection<String> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossVariantsQueryBuilder fields(String... fields) {
        return clearFields().addFields(fields);
    }

    public <N extends Number> AcrossVariantsQueryBuilder addFields(Map<String, N> fields) {
        for (Map.Entry<String, N> entry : fields.entrySet())
            boostedFields.put(entry.getKey(), entry.getValue().floatValue());
        hasBoostedFields = true;
        return this;
    }

    public <N extends Number> AcrossVariantsQueryBuilder fields(Map<String, N> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossVariantsQueryBuilder value(String value) {
        this.value = value;
        return this;
    }

    public AcrossVariantsQueryBuilder analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public AcrossVariantsQueryBuilder lang(String lang) {
        this.lang = lang;
        return this;
    }

    public AcrossVariantsQueryBuilder script(String script) {
        this.script = script;
        return this;
    }

    public AcrossVariantsQueryBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public AcrossVariantsQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params parameters) throws IOException {
        if (boostedFields.isEmpty())
            throw new ElasticSearchIllegalArgumentException("["+AcrossVariantsQueryParser.NAME+"] no fields given");
        builder.startObject(AcrossVariantsQueryParser.NAME);
        builder.field("value", value);
        if (hasBoostedFields) {
            builder.startObject("fields");
            for (Map.Entry<String, Float> entry : boostedFields.entrySet())
                builder.field(entry.getKey(), entry.getValue());
            builder.endObject();
        } else {
            builder.startArray("fields");
            for (String field : boostedFields.keySet())
                builder.value(field);
            builder.endArray();
        }
        if (analyzer != null)
            builder.field("analyzer", analyzer);
        if (lang != null)
            builder.field("lang", lang);
        if (script != null)
            builder.field("script", script);
        if (params != null)
            builder.field("params", params);
        if (boost != -1)
            builder.field("boost", boost);
        builder.endObject();
    }

}
