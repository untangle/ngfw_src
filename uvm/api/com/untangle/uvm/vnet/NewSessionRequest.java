/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Base interface for all new session requests.
 */
public interface NewSessionRequest 
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

    /**
     * Attaches the given object to this session request.
     *
     * <p> An attached object may later be retrieved via the {@link #attachment
     * attachment} method of the session itself.  (The attached object is automatically
     * attached to the session, assuming the request is allowed).
     * Only one object may be attached at a time; invoking
     * this method causes any previous attachment to be discarded.  The current
     * attachment may be discarded by attaching <tt>null</tt>.  </p>
     *
     * @param  ob
     *         The object to be attached; may be <tt>null</tt>
     *
     * @return  The previously-attached object, if any,
     *          otherwise <tt>null</tt>
     */
    Object attach(Object ob);
    Object attach(String key, Object ob);

    /**
     * Retrieves the current attachment.  </p>
     *
     * @return  The object currently attached to this session,
     *          or <tt>null</tt> if there is no attachment
     */
    Object attachment();
    Object attachment(String key);

    /**
     * Attaches the given object to this session
     * This is visible and modifiable by all Apps
     *
     * <p> An attached object may later be retrieved via the {@link
     * #attachment attachment} method.  Only one object may be
     * attached at a time for a given key; invoking this method
     * causes any previous attachment to be discarded.  The
     * current attachment may be discarded by attaching <tt>null</tt>.
     *
     * @param key The string key; may be <tt>null</tt>
     * @param ob The object to be attached; may be <tt>null</tt>
     *
     * @return The previously-attached object, if any, otherwise
     *          <tt>null</tt>
     */
    Object globalAttach(String key, Object ob);

    /**
     * Retrieves the current attachment.
     *
     * @param key The string key; may be <tt>null</tt>
     * 
     * @return The object currently attached to this session, or
     *          <tt>null</tt> if there is no attachment
     */
    Object globalAttachment(String key);
}
