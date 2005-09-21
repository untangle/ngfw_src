/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.security.LoginSession;

class LoginDesc
{
    private final LoginSession loginSession;

    private final Map<Integer, TargetDesc> targets
        = new ConcurrentHashMap<Integer, TargetDesc>();
    private final Map<Object, Map<Class, TargetDesc>> proxies
        = new WeakHashMap<Object, Map<Class, TargetDesc>>();

    private volatile Date lastAccess;
    private volatile LoginSession loginThief;

    private int lastId = 0;

    // constructors -----------------------------------------------------------

    LoginDesc(LoginSession loginSession)
    {
        this.loginSession = loginSession;
        this.lastAccess = new Date();
    }

    // package protected methods ----------------------------------------------

    TargetDesc getTargetDesc(Object target, Class c, TargetReaper targetReaper)
    {
        TargetDesc targetDesc = null;
        synchronized (proxies) {
            Map <Class, TargetDesc> ctd = proxies.get(target);
            if (null == ctd) {
                ctd = new HashMap<Class, TargetDesc>();
                proxies.put(target, ctd);
            } else {
                targetDesc = ctd.get(c);
            }

            if (null == targetDesc) {
                final int targetId = ++lastId;

                Runnable r = new Runnable()
                    {
                        public void run()
                        {
                            targets.remove(targetId);
                        }
                    };

                WeakReference tRef = targetReaper.makeReference(target, r);

                targetDesc = new TargetDesc(loginSession, targetId, tRef, c);

                targets.put(targetId, targetDesc);
                ctd.put(c, targetDesc);
            }
        }

        return targetDesc;
    }

    TargetDesc getTargetDesc(int targetId)
    {
        return targets.get(targetId);
    }

    void touch()
    {
        lastAccess = new Date();
    }

    void steal(LoginSession loginThief)
    {
        this.loginThief = loginThief;
            targets.clear();
        synchronized (proxies) {
            proxies.clear();
        }
    }

    LoginSession getLoginThief()
    {
        return loginThief;
    }

    boolean isStolen()
    {
        return null != loginThief;
    }

    Date getLastAccess()
    {
        return lastAccess;
    }
}
