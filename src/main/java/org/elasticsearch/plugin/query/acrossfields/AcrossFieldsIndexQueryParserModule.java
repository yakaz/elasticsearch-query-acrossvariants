package org.elasticsearch.plugin.query.acrossfields;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.index.query.RegisterAcrossFieldsQueryParsers;

public class AcrossFieldsIndexQueryParserModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RegisterAcrossFieldsQueryParsers.class).asEagerSingleton();
    }
}
