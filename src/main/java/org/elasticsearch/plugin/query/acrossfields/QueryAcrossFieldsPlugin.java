package org.elasticsearch.plugin.query.acrossfields;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

public class QueryAcrossFieldsPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "query-acrossfields";
    }

    @Override
    public String description() {
        return "Across fields query capabilities";
    }

    @Override
    public Collection<Class<? extends Module>> indexModules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        modules.add(AcrossFieldsIndexQueryParserModule.class);
        return modules;
    }
}
