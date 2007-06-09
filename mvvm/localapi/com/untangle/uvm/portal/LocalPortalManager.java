/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.portal;

import java.util.List;
import java.util.Set;

import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformStats;

/**
 * Local interface to the PortalManager.
 *
 * @author <a href="mailto:jdi@slabuntu">John Irwin</a>
 * @version 1.0
 */
public interface LocalPortalManager
{
    // This sux.  Duped in Transform and needs to be app independent. XXXXX
    static final int PROXY_COUNTER = Transform.GENERIC_1_COUNTER;
    static final int CIFS_COUNTER = Transform.GENERIC_2_COUNTER;

    // This one is shared by both remote desktop apps
    static final int FORWARD_COUNTER =  Transform.GENERIC_3_COUNTER;

    /**
     * The list of all bookmarks for the given user.  Is sorted with
     * user's bookmarks at top, then group (if any), then global.
     *
     * @return the list of all bookmarks for the user
     * @param user a <code>PortalUser</code> giving the user to fetch for
     */
    List<Bookmark> getAllBookmarks(PortalUser user);

    /**
     * Gets the <code>PortalHomeSettings</code> for the given user.
     * This finds the most specific one that is set and returns it.
     * (There is always a global one, so null is never returned)
     *
     * @param user a <code>PortalUser</code> giving the user to fetch for
     * @return the <code>PortalHomeSettings</code> for the user
     */
    PortalHomeSettings getPortalHomeSettings(PortalUser user);

    /**
     * Adds a new bookmark for the given user.  Does not currently
     * validate the target.
     */
    Bookmark addUserBookmark(PortalUser user, String name,
                             Application application, String target);

    Bookmark editUserBookmark(PortalUser user, Long id, String name,
                              Application application, String target);

    void removeUserBookmark(PortalUser user, Bookmark bookmark);

    void removeUserBookmarks(PortalUser user, Set<Long> ids);

    /**
     * Looks up a user by uid.  Returns null if the user does not exist.
     *
     * @param uid a <code>String</code> giving the uid of the user
     * @return a <code>PortalUser</code> value
     */
    PortalUser getUser(String uid);

    void logout(PortalLogin login);

    LocalApplicationManager applicationManager();

    PortalSettings getPortalSettings();

    void destroyPortalSettings();

    void setPortalSettings(PortalSettings settings);

    List<PortalLogin> getActiveLogins();

    void forceLogout(PortalLogin login);

    // Need this for the transform to give to UI.
    EventLogger getEventLogger(TransformContext tctx);

    // Need this for the transform to give to UI.
    TransformStats getStats();

    // Need this for webapps to bling
    void incrementStatCounter(int num);
}
