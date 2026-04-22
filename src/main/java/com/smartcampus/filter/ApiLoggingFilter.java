package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - API Request and Response Logging Filter
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ApiLoggingFilter.class.getName());

    /**
     * Called for every incoming request BEFORE the resource method executes.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.info(">>> INCOMING REQUEST  | Method: " + requestContext.getMethod()
                + " | URI: " + requestContext.getUriInfo().getRequestUri());
    }

    /**
     * Called for every outgoing response AFTER the resource method executes.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOG.info("<<< OUTGOING RESPONSE | Status: " + responseContext.getStatus()
                + " | URI: " + requestContext.getUriInfo().getRequestUri());
    }
}
