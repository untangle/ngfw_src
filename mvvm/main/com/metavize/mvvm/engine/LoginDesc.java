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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.security.LoginSession;

class LoginDesc
{
    private final LoginSession loginSession;

    private final Map<Integer, TargetDesc> targets
        = new ConcurrentHashMap<Integer, TargetDesc>();
    private final Map<Object, TargetDesc> proxies
        = new WeakHashMap<Object, TargetDesc>();

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

    TargetDesc getTargetDesc(Object target, TargetReaper targetReaper)
    {
        TargetDesc targetDesc;
        synchronized (proxies) {
            targetDesc = proxies.get(target);

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

                targetDesc = new TargetDesc(loginSession, targetId, tRef);

                targets.put(targetId, targetDesc);
                proxies.put(target, targetDesc);
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
        proxies.clear();
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
