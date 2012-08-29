/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.node.SessionTuple;

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

    /**
     * <code>argonConnector</code> returns the Meta Pipe <code>ArgonConnector</code>
     * that this session lives on.
     *
     * @return the <code>ArgonConnector</code> that this session is for
     */
    ArgonConnector argonConnector();

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
     * All sessions have a unique id assigned by Argon.  This will eventually, of course,
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
    
    public final String KEY_SITEFILTER_BEST_CATEGORY_ID = "esoft-best-category-id"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_NAME = "esoft-best-category-name"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_DESCRIPTION = "esoft-best-category-description"; /* String */
    public final String KEY_SITEFILTER_BEST_CATEGORY_FLAGGED = "esoft-best-category-flagged"; /* Boolean */
    public final String KEY_SITEFILTER_BEST_CATEGORY_BLOCKED = "esoft-best-category-blocked"; /* String */
    public final String KEY_SITEFILTER_FLAGGED = "esoft-flagged"; /* Boolean */
    public final String KEY_SITEFILTER_CATEGORIES = "esoft-categories"; /* List<String> */

    //public final String KEY_WEBFILTER_BEST_CATEGORY_ID = "untangle-best-category-id"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_NAME = "untangle-best-category-name"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_DESCRIPTION = "untangle-best-category-description"; /* String */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_FLAGGED = "untangle-best-category-flagged"; /* Boolean */
    //public final String KEY_WEBFILTER_BEST_CATEGORY_BLOCKED = "untangle-best-category-blocked"; /* String */
    //public final String KEY_WEBFILTER_FLAGGED = "untangle-flagged"; /* Boolean */
    //public final String KEY_WEBFILTER_CATEGORIES = "untangle-categories"; /* List<String> */

    public final String KEY_CLASSD_APPLICATION = "classd-application"; /* String */
    public final String KEY_CLASSD_CATEGORY = "classd-category"; /* String */
    public final String KEY_CLASSD_PROTOCHAIN = "classd-protochain"; /* String */
    public final String KEY_CLASSD_DETAIL = "classd-detail"; /* String */
    public final String KEY_CLASSD_CONFIDENCE = "classd-confidence"; /* Integer */
    public final String KEY_CLASSD_PRODUCTIVITY = "classd-productivity"; /* Integer */
    public final String KEY_CLASSD_RISK = "classd-risk"; /* Integer */

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
     */
    short getProtocol();

    /**
     * Returns an argon interface for the client.</p>
     *
     * @return a <code>int</code> giving the client interface of the session.
     */
    int getClientIntf();

    /**
     * Returns an argon interface for the server.</p>
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
}

