/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.Tag;
import com.untangle.uvm.app.SessionEvent;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.List;
import java.net.InetAddress;

/**
 * The base Sessoin interface
 */
public interface AppSession
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
    Object attach(String key, Object ob);

    /**
     * Retrieves the current attachment.
     *
     * @return The object currently attached to this session, or
     *          <tt>null</tt> if there is no attachment
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
     * Retrieves an attachment from the global session attachment table
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
    long getSessionId();
    
    /**
     * User identified for the session.  May be null, which means
     * that no user could be idenitifed for the session.
     *
     */
    String user();

    /**
     * The following are attachment keys used by various apps to
     * share information with other apps.
     */
    public final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE = "application-control-lite-protocol"; /* String */
    public final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_CATEGORY = "application-control-lite-category"; /* String */
    public final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_DESCRIPTION = "application-control-lite-description"; /* String */
    public final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_MATCHED = "application-control-lite-matched";  /* Boolean */

    public final String KEY_HTTP_HOSTNAME = "http-hostname";  /* String */
    public final String KEY_HTTP_REFERER = "http-referer";  /* String */
    public final String KEY_HTTP_URI = "http-uri";  /* String */
    public final String KEY_HTTP_URL = "http-url";  /* String */
    public final String KEY_HTTP_USER_AGENT = "http-user-agent";  /* String */
    public final String KEY_HTTP_CONTENT_TYPE = "http-content-type"; /* String */
    public final String KEY_HTTP_CONTENT_LENGTH = "http-content-length"; /* Long */
    public final String KEY_HTTP_REQUEST_METHOD = "http-request-method"; /* String */
    public final String KEY_HTTP_REQUEST_FILE_PATH = "http-request-file-path"; /* String */
    public final String KEY_HTTP_REQUEST_FILE_NAME = "http-request-file-name"; /* String */
    public final String KEY_HTTP_REQUEST_FILE_EXTENSION = "http-request-file-extension"; /* String */
    public final String KEY_HTTP_RESPONSE_FILE_NAME = "http-response-file-name"; /* String */
    public final String KEY_HTTP_RESPONSE_FILE_EXTENSION = "http-response-file-extension"; /* String */
    
    public final String KEY_FTP_FILE_NAME = "ftp-file-name";  /* String */
    public final String KEY_FTP_DATA_SESSION = "ftp-data-session";  /* Boolean */
    
    public final String KEY_WEB_FILTER_BEST_CATEGORY_ID = "web-filter-best-category-id"; /* String */
    public final String KEY_WEB_FILTER_BEST_CATEGORY_NAME = "web-filter-best-category-name"; /* String */
    public final String KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION = "web-filter-best-category-description"; /* String */
    public final String KEY_WEB_FILTER_BEST_CATEGORY_FLAGGED = "web-filter-best-category-flagged"; /* Boolean */
    public final String KEY_WEB_FILTER_BEST_CATEGORY_BLOCKED = "web-filter-best-category-blocked"; /* String */
    public final String KEY_WEB_FILTER_FLAGGED = "web-filter-flagged"; /* Boolean */
    public final String KEY_WEB_FILTER_CATEGORIES = "web-filter-categories"; /* List<String> */

    public final String KEY_APPLICATION_CONTROL_APPLICATION = "application-control-application"; /* String */
    public final String KEY_APPLICATION_CONTROL_CATEGORY = "application-control-category"; /* String */
    public final String KEY_APPLICATION_CONTROL_PROTOCHAIN = "application-control-protochain"; /* String */
    public final String KEY_APPLICATION_CONTROL_DETAIL = "application-control-detail"; /* String */
    public final String KEY_APPLICATION_CONTROL_CONFIDENCE = "application-control-confidence"; /* Integer */
    public final String KEY_APPLICATION_CONTROL_PRODUCTIVITY = "application-control-productivity"; /* Integer */
    public final String KEY_APPLICATION_CONTROL_RISK = "application-control-risk"; /* Integer */

    public final String KEY_SSL_INSPECTOR_SNI_HOSTNAME = "ssl-sni-host"; /* String */
    public final String KEY_SSL_INSPECTOR_SUBJECT_DN = "ssl-subject-dn"; /* String */
    public final String KEY_SSL_INSPECTOR_ISSUER_DN = "ssl-issuer-dn"; /* String */
    public final String KEY_SSL_INSPECTOR_CLIENT_MANAGER = "ssl-client-manager"; /* HttpsManager */
    public final String KEY_SSL_INSPECTOR_SERVER_MANAGER = "ssl-server-manager"; /* HttpsManager */
    public final String KEY_SSL_INSPECTOR_SESSION_INSPECT = "ssl-session-inspect"; /* Boolean */

    public final String KEY_WEB_FILTER_SSL_ENGINE = "web-filter-ssl-engine"; /* WebFilterSSLEngine */
    public final String KEY_CAPTIVE_PORTAL_REDIRECT = "captive-portal-redirect-client"; /* InetAddress */
    public final String KEY_CAPTIVE_PORTAL_SSL_ENGINE = "captive-portal-ssl-engine"; /* CaptureSSLEngine */
    public final String KEY_CAPTIVE_PORTAL_SESSION_CAPTURE = "captive-portal-session-capture"; /* Boolean */

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (right now always TCP or UDP)
     */
    short getProtocol();

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
     * Return the policy Id for this session
     */
    long getPolicyId();
    
    /**
     * Gets the original (pre-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getOrigClientAddr();

    /**
     * Gets the new (post-NAT) Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress getNewClientAddr();
    
    /**
     * Gets the original (pre-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getOrigServerAddr();

    /**
     * Gets the new (post-NAT) Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress getNewServerAddr();
    
    /**
     * Gets the original (pre-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getOrigClientPort();

    /**
     * Gets the new (post-NAT) client port for this session.</p>
     * @return the client port.
     */
    int getNewClientPort();
    
    /**
     * Gets the original (pre-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getOrigServerPort();

    /**
     * Gets the new (post-NAT) server port for this session.</p>
     * @return the server port.
     */
    int getNewServerPort();

    /**
     * Get the attachments to this session
     */
    Map<String,Object> getAttachments();

    /**
     * Kill/Reset this session
     */
    void killSession();

    /**
     * <code>release</code> releases all interest in all non-final events for this session.
     *
     * This call is only valid while in NORMAL_MODE.
     * Note: Just calls release(true);
     *
     */
    void release();

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

    void simulateClientData(ByteBuffer data);
    void simulateServerData(ByteBuffer data);

    void sendObjectToClient( Object obj );
    void sendObjectToServer( Object obj );
    void sendObject( int side, Object obj );

    void sendObjectsToClient( Object[] obj );
    void sendObjectsToServer( Object[] obj );
    void sendObjects( int side, Object[] obj );

    boolean hasTag( String name );
    void addTag( Tag tag );
    List<Tag> getTags();
}

