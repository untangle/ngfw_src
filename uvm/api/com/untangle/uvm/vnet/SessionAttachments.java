/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * The base Sessoin interface
 */
public interface SessionAttachments
{
    /**
     * The following are attachment keys used by various apps to
     * share information with other apps.
     */
    public static final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE = "application-control-lite-protocol"; /* String */
    public static final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_CATEGORY = "application-control-lite-category"; /* String */
    public static final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_DESCRIPTION = "application-control-lite-description"; /* String */
    public static final String KEY_APPLICATION_CONTROL_LITE_SIGNATURE_MATCHED = "application-control-lite-matched";  /* Boolean */

    public static final String KEY_HTTP_HOSTNAME = "http-hostname";  /* String */
    public static final String KEY_HTTP_REFERER = "http-referer";  /* String */
    public static final String KEY_HTTP_URI = "http-uri";  /* String */
    public static final String KEY_HTTP_URL = "http-url";  /* String */
    public static final String KEY_HTTP_USER_AGENT = "http-user-agent";  /* String */
    public static final String KEY_HTTP_CONTENT_TYPE = "http-content-type"; /* String */
    public static final String KEY_HTTP_CONTENT_LENGTH = "http-content-length"; /* Long */
    public static final String KEY_HTTP_REQUEST_METHOD = "http-request-method"; /* String */
    public static final String KEY_HTTP_REQUEST_FILE_PATH = "http-request-file-path"; /* String */
    public static final String KEY_HTTP_REQUEST_FILE_NAME = "http-request-file-name"; /* String */
    public static final String KEY_HTTP_REQUEST_FILE_EXTENSION = "http-request-file-extension"; /* String */
    public static final String KEY_HTTP_RESPONSE_FILE_NAME = "http-response-file-name"; /* String */
    public static final String KEY_HTTP_RESPONSE_FILE_EXTENSION = "http-response-file-extension"; /* String */
    
    public static final String KEY_FTP_FILE_NAME = "ftp-file-name";  /* String */
    public static final String KEY_FTP_DATA_SESSION = "ftp-data-session";  /* Boolean */
    
    public static final String KEY_WEB_FILTER_BEST_CATEGORY_ID = "web-filter-best-category-id"; /* String */
    public static final String KEY_WEB_FILTER_BEST_CATEGORY_NAME = "web-filter-best-category-name"; /* String */
    public static final String KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION = "web-filter-best-category-description"; /* String */
    public static final String KEY_WEB_FILTER_BEST_CATEGORY_FLAGGED = "web-filter-best-category-flagged"; /* Boolean */
    public static final String KEY_WEB_FILTER_BEST_CATEGORY_BLOCKED = "web-filter-best-category-blocked"; /* String */
    public static final String KEY_WEB_FILTER_FLAGGED = "web-filter-flagged"; /* Boolean */
    public static final String KEY_WEB_FILTER_CATEGORIES = "web-filter-categories"; /* List<String> */

    public static final String KEY_APPLICATION_CONTROL_APPLICATION = "application-control-application"; /* String */
    public static final String KEY_APPLICATION_CONTROL_CATEGORY = "application-control-category"; /* String */
    public static final String KEY_APPLICATION_CONTROL_PROTOCHAIN = "application-control-protochain"; /* String */
    public static final String KEY_APPLICATION_CONTROL_DETAIL = "application-control-detail"; /* String */
    public static final String KEY_APPLICATION_CONTROL_CONFIDENCE = "application-control-confidence"; /* Integer */
    public static final String KEY_APPLICATION_CONTROL_PRODUCTIVITY = "application-control-productivity"; /* Integer */
    public static final String KEY_APPLICATION_CONTROL_RISK = "application-control-risk"; /* Integer */

    public static final String KEY_SSL_INSPECTOR_SNI_HOSTNAME = "ssl-sni-host"; /* String */
    public static final String KEY_SSL_INSPECTOR_SUBJECT_DN = "ssl-subject-dn"; /* String */
    public static final String KEY_SSL_INSPECTOR_ISSUER_DN = "ssl-issuer-dn"; /* String */
    public static final String KEY_SSL_INSPECTOR_CLIENT_MANAGER = "ssl-client-manager"; /* HttpsManager */
    public static final String KEY_SSL_INSPECTOR_SERVER_MANAGER = "ssl-server-manager"; /* HttpsManager */
    public static final String KEY_SSL_INSPECTOR_SESSION_INSPECT = "ssl-session-inspect"; /* Boolean */

    public static final String KEY_WEB_FILTER_SSL_ENGINE = "web-filter-ssl-engine"; /* WebFilterSSLEngine */
    public static final String KEY_CAPTIVE_PORTAL_REDIRECT = "captive-portal-redirect-client"; /* InetAddress */
    public static final String KEY_CAPTIVE_PORTAL_SSL_ENGINE = "captive-portal-ssl-engine"; /* CaptureSSLEngine */
    public static final String KEY_CAPTIVE_PORTAL_SESSION_CAPTURE = "captive-portal-session-capture"; /* String */

    public static final String KEY_THREAT_PREVENTION_CLIENT_REPUTATION = "ip-reputation-client-reputation"; /* Integer */
    public static final String KEY_THREAT_PREVENTION_CLIENT_THREATMASK = "ip-reputation-client-threatmask"; /* Integer */
    public static final String KEY_THREAT_PREVENTION_SERVER_REPUTATION = "ip-reputation-server-reputation"; /* Integer */
    public static final String KEY_THREAT_PREVENTION_SERVER_THREATMASK = "ip-reputation-server-threatmask"; /* Integer */

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

}

