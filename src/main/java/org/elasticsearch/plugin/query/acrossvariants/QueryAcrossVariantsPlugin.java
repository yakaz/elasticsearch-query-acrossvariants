package org.elasticsearch.plugin.query.acrossvariants;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

public class QueryAcrossVariantsPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "query-acrossvariants";
    }

    @Override
    public String description() {
        return "Decompounding-variants aware, across fields, conjunctive query capabilities";
    }

    @Override
    public Collection<Class<? extends Module>> indexModules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        modules.add(AcrossVariantsIndexQueryParserModule.class);
        return modules;
    }
}
