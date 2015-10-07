package com.griddelta.lyza;

import io.dropwizard.Configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.datastax.driver.core.ConsistencyLevel;

public class LyzaConfiguration extends Configuration {
    @NotEmpty
    @NotNull
    private String cassandraHost;

    @Min(1)
    @Max(65535)
    private int cassandraPort = 9042;

    @NotEmpty
    @NotNull
    private String sparkMasterUrl;

    public String getCassandraHost() {
        return cassandraHost;
    }

    public int getCassandraPort() {
       return cassandraPort;
    }

    public String getSparkMasterUrl() {
        return sparkMasterUrl;
     }
    
    public ConsistencyLevel getConsistencyLevel(){
        // TOOD: Make this configurable
        return ConsistencyLevel.QUORUM;
    }
}