/*
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * The base Sessoin interface
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface Session
{

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
     * The following are attachment keys used by various nodes to
     * share information with other nodes.
     */
    public final String KEY_PLATFORM_ADCONNECTOR_USERNAME = "platform-adconnector-username"; /* String */

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
    public final String KEY_CLASSD_APPLICATION_CATEGORY = "classd-application-category"; /* String */
    public final String KEY_CLASSD_PROTOCHAIN = "classd-protochain"; /* String */
    public final String KEY_CLASSD_DETAIL = "classd-detail"; /* String */
    public final String KEY_CLASSD_CONFIDENCE = "classd-confidence"; /* Integer */

}

