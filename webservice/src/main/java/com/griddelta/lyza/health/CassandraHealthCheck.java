package com.griddelta.lyza.health;

import org.json.simple.JSONArray;

import com.codahale.metrics.health.HealthCheck;
import com.griddelta.lyza.LyzaApplication;
import com.griddelta.lyza.LyzaConfiguration;

public class CassandraHealthCheck extends HealthCheck {
	private LyzaApplication service;
    private LyzaConfiguration configuration;

	public CassandraHealthCheck(LyzaApplication service, LyzaConfiguration configuration) {
		this.service = service;
        this.configuration = configuration;
	}

	@Override
	public Result check() throws Exception {
		Result result = null;
		
		try {
			String host = configuration.getCassandraHost();
            int port = configuration.getCassandraPort();
			JSONArray keyspaces = this.service.getStorage().getKeyspaces();
			String output = "Connected to [" + host + ":" + port + "] w/ " + keyspaces.size() + " keyspaces.";
			result = Result.healthy(output);
		} catch (Throwable e) {
			result = Result.unhealthy("Unable to connect to cluster: "
					+ e.getMessage());
		}
		return result;
	}
}
