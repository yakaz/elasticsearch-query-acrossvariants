package org.elasticsearch.index.query;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AcrossFieldsFilterBuilder extends BaseFilterBuilder {

    private Collection<String> fields = new ArrayList<String>();
    private String value;
    private String analyzer;
    private String lang;
    private String script;
    Map<String, Object> params;

    public AcrossFieldsFilterBuilder(Collection<String> fields, String value, String analyzer) {
        this.fields = fields;
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossFieldsFilterBuilder(String value, String analyzer) {
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossFieldsFilterBuilder(String value) {
        this.value = value;
    }

    public AcrossFieldsFilterBuilder() {
    }

    public AcrossFieldsFilterBuilder clearFields() {
        fields.clear();
        return this;
    }

    public AcrossFieldsFilterBuilder addField(String field) {
        fields.add(field);
        return this;
    }

    public AcrossFieldsFilterBuilder field(String field) {
        return clearFields().addField(field);
    }

    public AcrossFieldsFilterBuilder addFields(Collection<String> fields) {
        for (String field : fields)
            this.fields.add(field);
        return this;
    }

    public AcrossFieldsFilterBuilder addFields(String... fields) {
        for (String field : fields)
            this.fields.add(field);
        return this;
    }

    public AcrossFieldsFilterBuilder fields(Collection<String> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossFieldsFilterBuilder fields(String... fields) {
        return clearFields().addFields(fields);
    }

    public AcrossFieldsFilterBuilder value(String value) {
        this.value = value;
        return this;
    }

    public AcrossFieldsFilterBuilder analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public AcrossFieldsFilterBuilder lang(String lang) {
        this.lang = lang;
        return this;
    }

    public AcrossFieldsFilterBuilder script(String script) {
        this.script = script;
        return this;
    }

    public AcrossFieldsFilterBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        if (fields.isEmpty())
            throw new QueryBuilderException("["+AcrossFieldsFilterParser.NAME+"] no fields given");
        builder.startObject(AcrossFieldsFilterParser.NAME);
        builder.field("value", value);
        builder.startArray("fields");
        for (String field : fields)
            builder.value(field);
        builder.endArray();
        if (analyzer != null)
            builder.field("analyzer", analyzer);
        if (lang != null)
            builder.field("lang", lang);
        if (script != null)
            builder.field("script", script);
        if (params != null)
            builder.field("params", params);
        builder.endObject();
    }

}
