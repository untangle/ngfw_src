/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.LocalUvmContextFactory;
import org.apache.log4j.Logger;

/**
 * Cleans unreferenced targets from the target cache.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
        thread = LocalUvmContextFactory.context().newThread(this);
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
