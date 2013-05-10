/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.SessionEvent;

import java.nio.ByteBuffer;
import java.util.Map;
import java.net.InetAddress;

/**
 * The base Sessoin interface
 */
public interface NodeSession extends SessionTuple
{
    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    public static final short PROTO_TCP = 6;
    public static final short PROTO_UDP = 17;

    static final byte CLOSED = 0;
    static final byte EXPIRED = 0;
    static final byte OPEN = 4;
    static final byte HALF_OPEN_INPUT = 5; /* for TCP */
    static final byte HALF_OPEN_OUTPUT = 6; /* for TCP */
    
    /**
     * <code>pipelineConnector</code> returns the Meta Pipe <code>PipelineConnector</code>
     * that this session lives on.
     *
     * @return the <code>PipelineConnector</code> that this session is for
     */
    PipelineConnector pipelineConnector();

    /**
     * Attaches the given object to this session.
     *
     * <p> An attached object may later be retrieved via the {@link
     * #attachment attachment} method.  Only one object may be
     * attached at a time; invoking this method causes any previous
     * attachment to be discarded.  The current attachment may be
     * discarded by attaching <tt>null</tt>.
     *
     * @param ob The object to be attached; may be <tt>null</tt>
     *
     * @return The previously-attached object, if any, otherwise
     *          <tt>null</tt>
     */
    Object attach(Object ob);

    /**
     * Retrieves the current attachment.
     *
     * @return The object currently attached to this session, or
     *          <tt>null</tt> if there is no attachment
     */
    Object attachment();

    /**
     * Attaches the given object to this session
     * This is visible and modifiable by all Nodes
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
     * User identified for the session.  May be null, which means
     * that no user could be idenitifed for the session.
     *
     */
    String user();
    
    /**
     * The following are attachment keys used by various nodes to
     * share information with other nodes.
     */
    public final String KEY_PLATFORM_USERNAME = "platform-username"; /* String */
    public final String KEY_PLATFORM_HOSTNAME = "platform-hostname"; /* String */

    public final String KEY_PROTOFILTER_SIGNATURE = "protofilter-protocol"; /* String */
    public final String KEY_PROTOFILTER_SIGNATURE_CATEGORY = "protofilter-category"; /* String */
    public final String KEY_PROTOFILTER_SIGNATURE_DESCRIPTION = "protofilter-description"; /* String */
    public final String KEY_PROTOFILTER_SIGNATURE_MATCHED = "protofilter-matched";  /* Boolean */

    public final String KEY_HTTP_HOSTNAME = "http-hostname";  /* String */
    public final String KEY_HTTP_URI = "http-uri";  /* String */
    public final String KEY_HTTP_CONTENT_TYPE = "http-content-type"; /* String */
    public final String KEY_HTTP_CONTENT_LENGTH = "http-content-length"; /* Long */
    
    public final String KEY_SITEFILTER_BEST_CATEGORY_ID = "sitefilter-best-category-id"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_NAME = "sitefilter-best-category-name"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_DESCRIPTION = "sitefilter-best-category-description"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_FLAGGED = "sitefilter-best-category-flagged"; /* Boolean */
    public final String KEY_SITEFILTER_BEST_CATEGORY_BLOCKED = "sitefilter-best-category-blocked"; /* String */
    public final String KEY_SITEFILTER_FLAGGED = "sitefilter-flagged"; /* Boolean */
    public final String KEY_SITEFILTER_CATEGORIES = "sitefilter-categories"; /* List<String> */

    //public final String KEY_WEBFILTER_BEST_CATEGORY_ID = "webfilter-best-category-id"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_NAME = "webfilter-best-category-name"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_DESCRIPTION = "webfilter-best-category-description"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_FLAGGED = "webfilter-best-category-flagged"; /* Boolean */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_BLOCKED = "webfilter-best-category-blocked"; /* String */
    //public final String KEY_WEBFILTER_FLAGGED = "webfilter-flagged"; /* Boolean */
    //public final String KEY_WEBFILTER_FLAGGED = "webfilter-content-type"; /* String */
    //public final String KEY_WEBFILTER_CATEGORIES = "webfilter-categories"; /* List<String> */

    public final String KEY_CLASSD_APPLICATION = "classd-application"; /* String */
    public final String KEY_CLASSD_CATEGORY = "classd-category"; /* String */
    public final String KEY_CLASSD_PROTOCHAIN = "classd-protochain"; /* String */
    public final String KEY_CLASSD_DETAIL = "classd-detail"; /* String */
    public final String KEY_CLASSD_CONFIDENCE = "classd-confidence"; /* Integer */
    public final String KEY_CLASSD_PRODUCTIVITY = "classd-productivity"; /* Integer */
    public final String KEY_CLASSD_RISK = "classd-risk"; /* Integer */

    public final String KEY_HTTPS_CLIENT_MANAGER = "https-client-manager"; /* HttpsManager */
    public final String KEY_HTTPS_SERVER_MANAGER = "https-server-manager"; /* HttpsManager */

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
     */
    short getProtocol();

    /**
     * Return the policy Id for this session
     */
    long getPolicyId();
    
    /**
     * Returns an netcap interface for the client.</p>
     *
     * @return a <code>int</code> giving the client interface of the session.
     */
    int getClientIntf();

    /**
     * Returns an netcap interface for the server.</p>
     *
     * @return a <code>int</code> giving the server interface of the session.
     */
    int getServerIntf();

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getClientAddr();

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getServerAddr();

    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    int getClientPort();

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    int getServerPort();

    /**
     * Get the attachments to this session
     */
    Map<String,Object> getAttachments();

    /**
     * Kill/Reset this session
     */
    void killSession();

    /**
     * <code>release</code> releases all interest in all non-final events for this session.  Only
     * the finalization event will be delievered, when the resulting session ends.
     *
     * This call is only valid while in NORMAL_MODE.
     * Note: Just calls release(true);
     *
     */
    void release();

    /**
     * <code>release</code> notifies the TAPI that this session may continue,
     * but no data events will be delivered for
     * the session.  If needsFinalization is false, no further events will be delivered for the session
     * at all.  IF needsFinalization is true, then the only event that will be delivered is a Finalization
     * event when the resulting session ends.
     *
     * @param needsFinalization a <code>boolean</code> true if the node needs a finalization event when the released session ends.
     */
    void release(boolean needsFinalization);

    /**
     * <code>scheduleTimer</code> sets the timer for this session to fire in
     * the given number of milliseconds. If the timer is already scheduled, it
     * the existing delay is discarded and the timer is rescheduled for the new
     * <code>delay</code>.
     *
     * @param delay a <code>long</code> giving milliseconds until the timer is to fire
     * @exception IllegalArgumentException if the delay is negative
     */
    void scheduleTimer(long delay) throws IllegalArgumentException;

    /**
     * <code>cancelTimer</code> cancels any scheduled timer expiration for this session.
     *
     */
    void cancelTimer();

    /**
     * <code>clientMark</code> returns the server-side socket mark for this session
     */
    int  clientMark();

    /**
     * <code>clientMark</code> sets the server-side socket mark for this session
     */
    void clientMark(int newmark);

    /**
     * <code>orClientMark</code> bitwise ORs the provided bitmask with the current client-side conn-mark
     */
    void orClientMark(int bitmask);

    /**
     * <code>setClientQosMark</code> sets the connmark so this session' client-side packets get the provided QoS priority
     */
    void setClientQosMark(int priority);
    
    /**
     * <code>serverMark</code> returns the server-side socket mark for this session
     */
    int  serverMark();

    /**
     * <code>serverMark</code> sets the server-side socket mark for this session
     */
    void serverMark(int newmark);

    /**
     * <code>orServerMark</code> bitwise ORs the provided bitmask with the current server-side conn-mark
     */
    void orServerMark(int bitmask);

    /**
     * <code>setServerQosMark</code> sets the connmark so this session' server-side packets get the provided QoS priority
     */
    void setServerQosMark(int priority);
    
    /**
     * Get the pipeline endpoints for this session
     */
    SessionEvent sessionEvent();

    byte clientState();
    byte serverState();

    NodeSessionStats stats();

    void simulateClientData(ByteBuffer data);
    void simulateServerData(ByteBuffer data);
}

