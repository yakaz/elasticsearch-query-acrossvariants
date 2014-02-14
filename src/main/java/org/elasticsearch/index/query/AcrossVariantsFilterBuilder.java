package org.elasticsearch.index.query;

import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AcrossVariantsFilterBuilder extends BaseFilterBuilder {

    private Collection<String> fields = new ArrayList<String>();
    private String value;
    private String analyzer;
    private String lang;
    private String script;
    Map<String, Object> params;

    public AcrossVariantsFilterBuilder(Collection<String> fields, String value, String analyzer) {
        this.fields = fields;
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossVariantsFilterBuilder(String value, String analyzer) {
        this.value = value;
        this.analyzer = analyzer;
    }

    public AcrossVariantsFilterBuilder(String value) {
        this.value = value;
    }

    public AcrossVariantsFilterBuilder() {
    }

    public AcrossVariantsFilterBuilder clearFields() {
        fields.clear();
        return this;
    }

    public AcrossVariantsFilterBuilder addField(String field) {
        fields.add(field);
        return this;
    }

    public AcrossVariantsFilterBuilder field(String field) {
        return clearFields().addField(field);
    }

    public AcrossVariantsFilterBuilder addFields(Collection<String> fields) {
        for (String field : fields)
            this.fields.add(field);
        return this;
    }

    public AcrossVariantsFilterBuilder addFields(String... fields) {
        for (String field : fields)
            this.fields.add(field);
        return this;
    }

    public AcrossVariantsFilterBuilder fields(Collection<String> fields) {
        return clearFields().addFields(fields);
    }

    public AcrossVariantsFilterBuilder fields(String... fields) {
        return clearFields().addFields(fields);
    }

    public AcrossVariantsFilterBuilder value(String value) {
        this.value = value;
        return this;
    }

    public AcrossVariantsFilterBuilder analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public AcrossVariantsFilterBuilder lang(String lang) {
        this.lang = lang;
        return this;
    }

    public AcrossVariantsFilterBuilder script(String script) {
        this.script = script;
        return this;
    }

    public AcrossVariantsFilterBuilder params(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params parameters) throws IOException {
        if (fields.isEmpty())
            throw new ElasticSearchIllegalArgumentException("["+AcrossVariantsFilterParser.NAME+"] no fields given");
        builder.startObject(AcrossVariantsFilterParser.NAME);
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
