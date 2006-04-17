/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.portal.*;

/**
 * Implementation of the PortalManager
 *
 */
public class PortalManagerImpl
  implements PortalManagerPriv {

    private static final Application[] protoArr = new Application[] { };

    private static final PortalManagerImpl PORTAL_MANAGER = new PortalManagerImpl();

    private final Logger logger = Logger.getLogger(PortalManagerImpl.class);

    private PortalApplicationManagerImpl appManager;

    private PortalSettings portalSettings;

    private PortalManagerImpl() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from PortalSettings");
                    portalSettings = (PortalSettings)q.uniqueResult();

                    if (null == portalSettings) {
                        portalSettings = new PortalSettings();
                        PortalGlobal pg = new PortalGlobal();
                        pg.setPortalHomeSettings(new PortalHomeSettings());
                        portalSettings.setGlobal(pg);
                        s.save(portalSettings);

                    }
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        appManager = PortalApplicationManagerImpl.applicationManager();

        logger.info("Initialized PortalManager");
    }

    /**
     * Do not call this directly, instead go through <code>MvvmLocalContext</code>
     */
    static PortalManagerImpl getInstance() {
        return PORTAL_MANAGER;
    }

    public PortalApplicationManagerImpl applicationManager() {
        return appManager;
    }

    public PortalSettings getPortalSettings()
    {
        return portalSettings;
    }

    public void setPortalSettings(final PortalSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        this.portalSettings = settings;
    }

    public List<Bookmark> getAllBookmarks(PortalUser user)
    {
        List<Bookmark> result = new ArrayList<Bookmark>();

        List userBookmarks = user.getBookmarks();
        if (userBookmarks != null)
            result.addAll(userBookmarks);

        PortalGroup userGroup = user.getPortalGroup();
        if (userGroup != null) {
            List groupBookmarks = userGroup.getBookmarks();
            if (groupBookmarks != null)
                result.addAll(groupBookmarks);
        }

        List globalBookmarks = portalSettings.getGlobal().getBookmarks();
        if (globalBookmarks != null)
            result.addAll(globalBookmarks);

        return result;
    }

    public PortalUser lookupUser(String uid)
    {
        List allUsers = portalSettings.getUsers();
        for (Iterator iter = allUsers.iterator(); iter.hasNext();) {
            PortalUser user = (PortalUser) iter.next();
            if (uid.equals(user.getUid()))
                return user;
        }

        if (portalSettings.getGlobal().isAutoCreateUsers()) {
            logger.debug("lookupUser: " + uid + " didn't exist, auto creating");
            PortalUser newUser = portalSettings.addUser(uid);
            setPortalSettings(portalSettings);
            return newUser;
        }

        return null;
    }
}
