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

package com.untangle.uvm.engine;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.UvmContextFactory;
import org.apache.log4j.Logger;

class TargetReaper implements Runnable
{
    private final Logger logger = Logger.getLogger(getClass());

    private final ReferenceQueue referenceQueue = new ReferenceQueue();
    private final Map<Reference, Runnable> actions
        = new HashMap<Reference, Runnable>();

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

            Runnable r;
            synchronized (actions) {
                r = actions.remove(ref);
            }
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
        thread = UvmContextFactory.context().newThread(this);
        thread.start();
    }

    void destroy()
    {
        Thread t = thread;
        if (t != null) {
            thread = null;
            t.interrupt();
        }
    }

    WeakReference makeReference(Object target, Runnable runnable)
    {
        WeakReference targetRef = new WeakReference(target, referenceQueue);
        synchronized (actions) {
            actions.put(targetRef, runnable);
        }
        return targetRef;
    }

    void removeReference(WeakReference ref)
    {
        actions.remove(ref);
    }
}
