package org.elasticsearch.index.query;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.script.ScriptService;

public class RegisterAcrossVariantsQueryParsers extends AbstractIndexComponent {

    @Inject
    public RegisterAcrossVariantsQueryParsers(Index index, @IndexSettings Settings indexSettings, IndicesQueriesRegistry indicesQueriesRegistry, Injector injector) {
        super(index, indexSettings);

        AnalysisService analysisService = injector.getInstance(AnalysisService.class);
        ScriptService scriptService = injector.getInstance(ScriptService.class);

        indicesQueriesRegistry.addQueryParser(new AcrossVariantsQueryParser(analysisService, scriptService));
        indicesQueriesRegistry.addFilterParser(new AcrossVariantsFilterParser(analysisService, scriptService));
    }
}
