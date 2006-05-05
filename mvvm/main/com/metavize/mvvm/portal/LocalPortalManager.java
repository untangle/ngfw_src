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

package com.metavize.mvvm.portal;

import java.net.InetAddress;
import java.util.List;

import com.metavize.mvvm.portal.*;

/**
 * Describe interface <code>LocalPortalManager</code> here.
 *
 * @author <a href="mailto:jdi@slabuntu">John Irwin</a>
 * @version 1.0
 */
public interface LocalPortalManager
{
    String PORTAL_LOGIN_KEY = "portal-login-key";

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
    Bookmark addUserBookmark(PortalUser user, String name, Application application, String target);

    void removeUserBookmark(PortalUser user, Bookmark bookmark);

    /**
     * Looks up a user by uid.  Returns null if the user does not exist.
     *
     * @param uid a <code>String</code> giving the uid of the user
     * @return a <code>PortalUser</code> value
     */
    PortalUser getUser(String uid);

    /**
     * Looks up a login by login key.  Returns null if the login does
     * not exist; this happens if the login times out or is forced to
     * log out.
     *
     * @param loginKey a <code>PortalLoginKey</code> giving the key
     * for the login
     * @return a <code>PortalLogin</code> value
     */
    PortalLogin getLogin(PortalLoginKey loginKey);

    PortalLoginKey login(String uid, String password, InetAddress clientAddr);

    /**
     * Normal user-initiated login.
     *
     * @param loginKey a <code>PortalLoginKey</code> giving the login
     * to log out
     */
    void logout(PortalLoginKey loginKey);

    LocalApplicationManager applicationManager();

    PortalSettings getPortalSettings();

    void setPortalSettings(PortalSettings settings);

    List<PortalLogin> getActiveLogins();

    void forceLogout(PortalLogin login);
 }
