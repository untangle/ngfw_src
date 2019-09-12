/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Base interface for all new session requests.
 */
public interface NewSessionRequest extends SessionAttachments
{
    /**
     * <code>id</code> returns the session's unique identifier, a positive integer >= 1.
     * All sessions have a unique id assigned by Netcap.  This will eventually, of course,
     * wrap around.  This will take long enough, and any super-long-lived sessions that
     * get wrapped to will not be duplicated, so the rollover is ok.
     *
     * @return an <code>int</code> giving the unique ID of the session.
     */
    long id();

    /**
     * <code>pipelineConnector</code> returns the Meta Pipe <code>PipelineConnector</code> that this session lives on.
     *
     * @return the <code>PipelineConnector</code> that this session is for
     */
    PipelineConnector pipelineConnector();

}
