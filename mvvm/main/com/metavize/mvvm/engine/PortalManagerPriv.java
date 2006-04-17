/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import java.util.List;
import com.metavize.mvvm.portal.*;

/**
 * Describe interface <code>PortalManagerPriv</code> here.
 *
 * @author <a href="mailto:jdi@slabuntu">John Irwin</a>
 * @version 1.0
 */
public interface PortalManagerPriv extends PortalManager
{

    /**
     * The list of all bookmarks for the given user.  Is sorted with user's bookmarks
     * at top, then group (if any), then global.
     *
     * @return the list of all bookmarks for the user
     * @param user a <code>PortalUser</code> giving the user to fetch for
     */
    List<Bookmark> getAllBookmarks(PortalUser user);

    /**
     * Gets the <code>PortalHomeSettings</code> for the given user.  This finds the
     * most specific one that is set and returns it.  (There is always a global one,
     * so null is never returned)
     *
     * @param user a <code>PortalUser</code> giving the user to fetch for
     * @return the <code>PortalHomeSettings</code> for the user
     */
    PortalHomeSettings getPortalHomeSettings(PortalUser user);

    /**
     * <code>lookupUser</code> should be called <b>after</b> the uid has been authenticated
     * by the AddressBook.  We look up the PortalUser, if it already exists it is returned.
     * Otherwise, if <code>isAutoCreateUsers</code> is on,  a new PortalUser is automatically
     * created and returned.
     * Otherwise, <code>null</code> is returned.
     *
     * Note that the resulting PortalUser must still be checked for liveness.
     *
     * @param uid a <code>String</code> giving the uid of the already authenticated user
     * @return a <code>PortalUser</code> value
     */
    PortalUser lookupUser(String uid);
    
 }
