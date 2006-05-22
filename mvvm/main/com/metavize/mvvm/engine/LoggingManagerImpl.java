/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.logging.LoggingSettings;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class LoggingManagerImpl implements LoggingManager
{
    private static final Object LOCK = new Object();
    private static final LogEvent[] LOG_PROTO = new LogEvent[0];

    private static LoggingManagerImpl LOGGING_MANAGER;

    private final Logger logger = Logger.getLogger(getClass());

    private LoggingSettings loggingSettings;

    private LoggingManagerImpl()
    {
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

    static LoggingManagerImpl loggingManager()
    {
        synchronized (LOCK) {
            if (null == LOGGING_MANAGER) {
                LOGGING_MANAGER = new LoggingManagerImpl();
            }
        }

        return LOGGING_MANAGER;
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
                    s.saveOrUpdate(loggingSettings);
                    return true;
                }
            };

        MvvmContextFactory.context().runTransaction(tw);

        SyslogManagerImpl.manager().reconfigure(loggingSettings);
    }

    public void resetAllLogs()
    {
        MvvmRepositorySelector.get().reconfigureAll();
    }
}
