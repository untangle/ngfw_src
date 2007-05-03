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

package com.untangle.mvvm.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.LoggingManager;
import com.untangle.mvvm.logging.LoggingSettings;
import com.untangle.mvvm.logging.MvvmRepositorySelector;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class LoggingManagerImpl implements LoggingManager
{
    private static final Object LOCK = new Object();
    private static final boolean LOGGING_DISABLED
        = Boolean.parseBoolean(System.getProperty("mvvm.logging.disabled"));

    private static LoggingManagerImpl LOGGING_MANAGER;

    private final List<String> initQueue = new LinkedList<String>();
    private final LogWorker logWorker = new LogWorker(this);
    private final MvvmRepositorySelector repositorySelector;

    private final Logger logger = Logger.getLogger(getClass());

    private LoggingSettings loggingSettings;

    private volatile boolean conversionComplete = true;

    LoggingManagerImpl(MvvmRepositorySelector repositorySelector)
    {
        this.repositorySelector = repositorySelector;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from LoggingSettings ls");
                    loggingSettings = (LoggingSettings)q.uniqueResult();

                    if (null == loggingSettings) {
                        loggingSettings = new LoggingSettings();
                        s.save(loggingSettings);
                    }

                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        SyslogManagerImpl.manager().reconfigure(loggingSettings);
    }

    static boolean isLoggingDisabled()
    {
        return LOGGING_DISABLED;
    }

    public LoggingSettings getLoggingSettings()
    {
        return loggingSettings;
    }

    public void setLoggingSettings(final LoggingSettings loggingSettings)
    {
        this.loggingSettings = loggingSettings;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(loggingSettings);
                    return true;
                }
            };

        MvvmContextFactory.context().runTransaction(tw);

        SyslogManagerImpl.manager().reconfigure(loggingSettings);
    }

    public void resetAllLogs()
    {
        repositorySelector.reconfigureAll();
    }

    public void logError(String errorText)
    {
        if (null == errorText) {
            logger.error("This is the default error text.");
        } else {
            logger.error(errorText);
        }

        return;
    }

    // package protected methods ----------------------------------------------

    void start()
    {
        logWorker.start();
    }

    void stop()
    {
        logWorker.stop();
    }

    BlockingQueue<LogEventDesc> getInputQueue()
    {
        return logWorker.getInputQueue();
    }

    void initSchema(final String name)
    {
        synchronized (initQueue) {
            conversionComplete = false;

            initQueue.add(name);
        }

        // XXX not using newThread, called from MvvmContextImpl constructor
        new Thread(new Runnable()
            {
                public void run()
                {
                    MvvmContextImpl mctx = MvvmContextImpl.getInstance();
                    mctx.waitForStartup();

                    synchronized (initQueue) {
                        for (Iterator<String> i = initQueue.iterator(); i.hasNext(); ) {
                            String n = i.next();
                            i.remove();
                            mctx.schemaUtil().initSchema("events", n);
                        }

                        conversionComplete = true;
                        initQueue.notifyAll();
                    }
                }
            }).start();
    }

    boolean isConversionComplete()
    {
        return conversionComplete;
    }
}
