/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: LoggingManagerImpl.java,v 1.7 2004/12/25 11:30:17 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.Tid;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class LoggingManagerImpl implements LoggingManager
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

    public LogEvent[] userLogs(Tid tid)
    {
        LogEvent[] logs = null;

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from LogEvent le where le.tid = :tid");
            q.setParameter("tid", tid);
            List result = q.list();

            logs = (LogEvent[])result.toArray(LOG_PROTO);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get LogEvents", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }

        return logs;
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
