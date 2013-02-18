package org.elasticsearch.index.query;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;

public class RegisterAcrossFieldsQueryParsers extends AbstractIndexComponent {

    @Inject
    public RegisterAcrossFieldsQueryParsers(Index index, @IndexSettings Settings indexSettings, IndicesQueriesRegistry indicesQueriesRegistry, Injector injector) {
        super(index, indexSettings);

        AnalysisService analysisService = injector.getInstance(AnalysisService.class);

        indicesQueriesRegistry.addQueryParser(new AcrossFieldsQueryParser(analysisService));
        indicesQueriesRegistry.addFilterParser(new AcrossFieldsFilterParser());
    }
}
