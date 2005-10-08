/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class LoggingManagerImpl implements LoggingManager
{
    private static final Object LOCK = new Object();
    private static final LogEvent[] LOG_PROTO = new LogEvent[0];

    private static LoggingManagerImpl LOGGING_MANAGER;

    private static final Logger logger = Logger
        .getLogger(LoggingManagerImpl.class.getName());

    private LoggingManagerImpl() { }

    static LoggingManagerImpl loggingManager()
    {
        synchronized (LOCK) {
            if (null == LOGGING_MANAGER) {
                LOGGING_MANAGER = new LoggingManagerImpl();
            }
        }
        return LOGGING_MANAGER;
    }

    public LogEvent[] userLogs(final Tid tid)
    {
        LogEvent[] logs = null;

        TransactionWork<LogEvent[]> tw = new TransactionWork<LogEvent[]>()
            {
                private LogEvent[] logs = null;

                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from LogEvent le where le.tid = :tid");
                    q.setParameter("tid", tid);
                    List result = q.list();

                    logs = (LogEvent[])result.toArray(LOG_PROTO);

                    return true;
                }

                public LogEvent[] getResult()
                {
                    return logs;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return tw.getResult();
    }

    public String[] userLogStrings(Tid tid)
    {
        LogEvent[] les = userLogs(tid);
        String[] sles = new String[les.length];
        for (int i = 0; i < sles.length; i++) {
            sles[i] = les[i].toString();
        }

        return sles;
    }

    public void resetAllLogs()
    {
        MvvmRepositorySelector.get().reconfigureAll();
    }
}
