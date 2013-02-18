package org.elasticsearch.index.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.elasticsearch.common.lucene.search.TermFilter;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.cache.filter.support.CacheKeyFilter;
import org.elasticsearch.index.mapper.MapperService;

import java.io.IOException;

import static org.elasticsearch.index.query.support.QueryParsers.wrapSmartNameFilter;

public class AcrossFieldsFilterParser implements FilterParser {

    public static final String NAME = "acrossfields";

    @Override
    public String[] names() {
        return new String[]{NAME};
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        // FIXME TODO Implement
        boolean cache = true; // since usually term filter is on repeating terms, cache it by default
        CacheKeyFilter.Key cacheKey = null;
        String fieldName = null;
        String value = null;

        String filterName = null;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else if ("_cache".equals(currentFieldName)) {
                    cache = parser.booleanValue();
                } else if ("_cache_key".equals(currentFieldName) || "_cacheKey".equals(currentFieldName)) {
                    cacheKey = new CacheKeyFilter.Key(parser.text());
                } else {
                    fieldName = currentFieldName;
                    value = parser.text();
                }
            }
        }

        if (fieldName == null) {
            throw new QueryParsingException(parseContext.index(), "No field specified for "+NAME+" filter");
        }

        if (value == null) {
            throw new QueryParsingException(parseContext.index(), "No value specified for "+NAME+" filter");
        }

        Filter filter = null;
        MapperService.SmartNameFieldMappers smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);

        // NO special mapper usage, just a plain, un-analyzed TermFilter!
        filter = new TermFilter(new Term(fieldName, value));

        if (cache) {
            filter = parseContext.cacheFilter(filter, cacheKey);
        }

        filter = wrapSmartNameFilter(filter, smartNameFieldMappers, parseContext);
        if (filterName != null) {
            parseContext.addNamedFilter(filterName, filter);
        }
        return filter;
    }

}
