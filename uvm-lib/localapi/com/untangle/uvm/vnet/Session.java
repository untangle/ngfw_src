/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet;

/**
 * The base Sessoin interface
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface Session extends SessionDesc
{

    /**
     * <code>mPipe</code> returns the Meta Pipe <code>MPipe</code>
     * that this session lives on.
     *
     * @return the <code>MPipe</code> that this session is for
     */
    MPipe mPipe();

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
    public final String KEY_PROTOFILTER_PROTOCOL = "protofilter-protocol";
    public final String KEY_PROTOFILTER_PROTOCOL_CATEGORY = "protofilter-category";
    public final String KEY_PROTOFILTER_PROTOCOL_DESCRIPTION = "protofilter-description";
    public final String KEY_SITEFILTER_BEST_CATEGORY_ID = "esoft-best-category-id";
    public final String KEY_SITEFILTER_BEST_CATEGORY_NAME = "esoft-best-category-name";
    public final String KEY_SITEFILTER_BEST_CATEGORY_DESCRIPTION = "esoft-best-category-description";
    public final String KEY_SITEFILTER_BEST_CATEGORY_LOGGED = "esoft-best-category-logged";
    public final String KEY_SITEFILTER_BEST_CATEGORY_BLOCKED = "esoft-best-category-blocked";
    public final String KEY_WEBFILTER_BEST_CATEGORY_ID = "untangle-best-category-id";
    public final String KEY_WEBFILTER_BEST_CATEGORY_NAME = "untangle-best-category-name";
    public final String KEY_WEBFILTER_BEST_CATEGORY_DESCRIPTION = "untangle-best-category-description";
    public final String KEY_WEBFILTER_BEST_CATEGORY_LOGGED = "untangle-best-category-logged";
    public final String KEY_WEBFILTER_BEST_CATEGORY_BLOCKED = "untangle-best-category-blocked";
    public final String KEY_PLATFORM_ADCONNECTOR_USERNAME = "platform-adconnector-username";

}

