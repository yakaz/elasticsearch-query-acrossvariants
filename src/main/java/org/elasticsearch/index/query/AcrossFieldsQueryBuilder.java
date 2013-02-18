package org.elasticsearch.index.query;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AcrossFieldsQueryBuilder extends BaseQueryBuilder {

    private Map<String, Float> boostedFields = new HashMap<String, Float>();
    private String value;
    private String analyzer;
    private String lang;
    private String script;
    Map<String, Object> params;
    private boolean hasBoostedFields = false;
    private float boost = -1;

    public AcrossFieldsQueryBuilder(Map<String, Float> boostedFields, String value, String analyzer) {
        this.boostedFields = boostedFields;
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossFieldsQueryBuilder(String value, String analyzer) {
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossFieldsQueryBuilder(String value) {
        this.value = value;
    }

    public AcrossFieldsQueryBuilder() {
    }

    public AcrossFieldsQueryBuilder clearFields() {
        boostedFields.clear();
        hasBoostedFields = false;
        return this;
    }

    public AcrossFieldsQueryBuilder addField(String field) {
        boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossFieldsQueryBuilder field(String field) {
        return clearFields().addField(field);
    }

    public AcrossFieldsQueryBuilder addField(String field, float boost) {
        boostedFields.put(field, boost);
        hasBoostedFields = true;
        return this;
    }

    public AcrossFieldsQueryBuilder field(String field, float boost) {
        return clearFields().addField(field, boost);
    }

    public AcrossFieldsQueryBuilder addFields(Collection<String> fields) {
        for (String field : fields)
            boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossFieldsQueryBuilder addFields(String... fields) {
        for (String field : fields)
            boostedFields.put(field, -1.0f);
        return this;
    }

    public AcrossFieldsQueryBuilder fields(Collection<String> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossFieldsQueryBuilder fields(String... fields) {
        return clearFields().addFields(fields);
    }

    public <N extends Number> AcrossFieldsQueryBuilder addFields(Map<String, N> fields) {
        for (Map.Entry<String, N> entry : fields.entrySet())
            boostedFields.put(entry.getKey(), entry.getValue().floatValue());
        hasBoostedFields = true;
        return this;
    }

    public <N extends Number> AcrossFieldsQueryBuilder fields(Map<String, N> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossFieldsQueryBuilder value(String value) {
        this.value = value;
        return this;
    }

    public AcrossFieldsQueryBuilder analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public AcrossFieldsQueryBuilder lang(String lang) {
        this.lang = lang;
        return this;
    }

    public AcrossFieldsQueryBuilder script(String script) {
        this.script = script;
        return this;
    }

    public AcrossFieldsQueryBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public AcrossFieldsQueryBuilder boost(float boost) {
        this.boost = boost;
        return this;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        if (boostedFields.isEmpty())
            throw new QueryBuilderException("["+AcrossFieldsQueryParser.NAME+"] no fields given");
        builder.startObject(AcrossFieldsQueryParser.NAME);
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
