package com.griddelta.lyza;


import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.griddelta.lyza.exception.KeyspaceExceptionMapper;
import com.griddelta.lyza.health.CassandraHealthCheck;
import com.griddelta.lyza.resource.DataResource;

public class LyzaApplication extends Application<LyzaConfiguration> {
    public static CassandraStorage storage = null;
    LyzaConfiguration config = null;

    public static void main(String[] args) throws Exception {    	
        LyzaApplication app = new LyzaApplication();
        app.run(args);        
    }

    @Override
    public void initialize(Bootstrap<LyzaConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/ui", "/foo"));
    }

    @Override
    public void run(LyzaConfiguration config, Environment env) throws Exception {    	
        this.config = config;
        this.setStorage(new CassandraStorage(config.getCassandraHost(), config.getCassandraPort()));
        env.jersey().register(new DataResource(this));
        env.healthChecks().register("Cassandra", new CassandraHealthCheck(this, config));
        env.jersey().register(new KeyspaceExceptionMapper());
    }

    public CassandraStorage getStorage() {
        return storage;
    }

    public void setStorage(CassandraStorage storage) {
        LyzaApplication.storage = storage;
    }

    public LyzaConfiguration getConfig() {
        return config;
    }

    public void setConfig(LyzaConfiguration config) {
        this.config = config;
    }
}
