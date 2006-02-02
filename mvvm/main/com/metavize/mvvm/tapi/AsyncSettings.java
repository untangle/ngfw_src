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
    private TransformContext tctx;
    private SettingsChangeListener<T> settingsChangeListener;
    private boolean loaded = false;
    private boolean initialized = false;
    private T settings = null;

    public void init(final TransformContext tctx, final String query,
                     final SettingsChangeListener<T> settingsChangeListener)
    {
        synchronized (this) {
            if (initialized) {
                throw new IllegalStateException("already initialized");
            } else {
                initialized = true;
            }
        }

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

    public void init(final TransformContext tctx, final String query) {
        init(tctx, query, null);
    }

    public void init(TransformContext tctx, T settings,
                     SettingsChangeListener settingsChangeListener)
    {
        synchronized (this) {
            if (initialized) {
                throw new IllegalStateException("already initialized");
            } else {
                initialized = true;
            }
        }

        this.tctx = tctx;
        this.loaded = true;
        this.settingsChangeListener = settingsChangeListener;

        setSettings(settings);
    }

    public void init(TransformContext tctx, T settings)
    {
        init(tctx, settings, null);
    }

    public boolean isInitialized()
    {
        synchronized (this) {
            return initialized;
        }
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
