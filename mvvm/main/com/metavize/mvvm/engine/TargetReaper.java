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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

class TargetReaper implements Runnable
{
    private static final Logger logger = Logger.getLogger(TargetReaper.class);

    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    private final Map<Reference, Runnable> actions
        = new ConcurrentHashMap<Reference, Runnable>();

    private volatile Thread thread;

    // Runnable methods -------------------------------------------------------

    public void run()
    {
        Thread thisThread = Thread.currentThread();

        while (thisThread == thread) {
            Reference ref;
            try {
                ref = referenceQueue.remove();
            } catch (InterruptedException exn) {
                continue;
            }

            Runnable r = actions.get(ref);
            if (null == r) {
                logger.warn("no action for reference: " + ref);
            } else {
                r.run();
            }
        }
    }

    // package protected methods ----------------------------------------------

    void init()
    {
        thread = new Thread(this);
        thread.start();
    }

    void destroy()
    {
        Thread t = thread;
        thread = null;
        t.interrupt();
    }

    WeakReference makeReference(Object target, Runnable runnable)
    {
        WeakReference targetRef = new WeakReference(target, referenceQueue);
        actions.put(targetRef, runnable);
        return targetRef;
    }
}
