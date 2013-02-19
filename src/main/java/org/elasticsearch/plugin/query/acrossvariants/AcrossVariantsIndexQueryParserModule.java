package org.elasticsearch.plugin.query.acrossvariants;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.index.query.RegisterAcrossVariantsQueryParsers;

public class AcrossVariantsIndexQueryParserModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RegisterAcrossVariantsQueryParsers.class).asEagerSingleton();
    }
}
