//
// Copyright (c) 2012 Health Market Science, Inc.
//
package com.griddelta.lyza.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.exceptions.InvalidConfigurationInQueryException;

@Provider
public class KeyspaceExceptionMapper implements ExceptionMapper<InvalidConfigurationInQueryException> {
	private static Logger LOG = LoggerFactory.getLogger(KeyspaceExceptionMapper.class);
	
    public Response toResponse(InvalidConfigurationInQueryException exception) {
    	LOG.error("Error", exception);
        return Response.status(Status.NOT_FOUND).entity(exception.getCause()).build();
    }
}
