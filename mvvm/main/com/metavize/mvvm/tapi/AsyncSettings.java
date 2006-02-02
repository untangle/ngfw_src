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

package com.metavize.mvvm.tapi;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;

public class AsyncSettings<T>
{
    private final TransformContext tctx;
    private final SettingsChangeListener<T> settingsChangeListener;

    private boolean loaded = false;
    private T settings = null;

    public AsyncSettings(final TransformContext tctx, final String query,
                         final SettingsChangeListener<T> settingsChangeListener)
    {
        this.tctx = tctx;
        this.settingsChangeListener = settingsChangeListener;

        new Thread(new Runnable()
            {
                public void run()
                {
                    TransactionWork tw = new TransactionWork()
                        {
                            public boolean doWork(Session s)
                            {
                                Query q = s.createQuery(query);
                                q.setParameter("tid", tctx.getTid());
                                settings = (T)q.uniqueResult();

                                return true;
                            }
                        };

                    try {
                        tctx.runTransaction(tw);
                        if (null != settingsChangeListener) {
                            settingsChangeListener.newSettings(settings);
                        }
                    } finally { // XXX store exception?
                        synchronized(this) {
                            loaded = true;
                            AsyncSettings.this.notifyAll();
                        }
                    }
                }
            }).start();
    }

    public AsyncSettings(final TransformContext tctx, final String query) {
        this(tctx, query, null);
    }

    public T getSettings()
    {
        synchronized (this) {
            while (!loaded) {
                try {
                    this.wait();
                } catch (InterruptedException exn) {
                    // loaded is set to abort
                }
            }
        }

        return settings;
    }

    public void setSettings(final T settings)
    {
        this.settings = settings;

        // XXX make async too?
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    if (null != settingsChangeListener) {
                        settingsChangeListener.newSettings(settings);
                    }

                    return true;
                }
            };
        tctx.runTransaction(tw);
    }
}
