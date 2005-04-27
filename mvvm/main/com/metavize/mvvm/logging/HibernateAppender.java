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

package com.metavize.mvvm.logging;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.TransformContext;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.logicalcobwebs.proxool.ConnectionPoolDefinitionIF;
import org.logicalcobwebs.proxool.ProxoolFacade;
import org.logicalcobwebs.proxool.ProxoolListenerIF;

public class HibernateAppender extends AppenderSkeleton
{
    private static final int QUEUE_SIZE = 1000;
    private static final int SLEEP_TIME = 60000;

    private static final Logger logger = Logger
        .getLogger(HibernateAppender.class);

    private final Map loggers = new ConcurrentHashMap();

    // constructors -----------------------------------------------------------

    public HibernateAppender()
    {
        ProxoolFacade.addProxoolListener(new ProxoolListenerIF()
            {
                public void onRegistration(ConnectionPoolDefinitionIF def,
                                           Properties props) { }


                public void onShutdown(String alias)
                {
                    if (alias.equals("mvvm")) {
                        logger.info("shutting down appender");
                        shutdownLoggers();
                    } else {
                        logger.warn("ignoring: " + alias);
                    }
                }
            });
    }

    // Appender methods -------------------------------------------------------

    protected void append(LoggingEvent event)
    {
        TransformContext tctx = TransformContextFactory.context();

        Tid tid = null == tctx ? new Tid(0L) : tctx.getTid();

        Object msg = event.getMessage();
        if (!(msg instanceof LogEvent)) {
            logger.warn("not logEvent: " + msg);
            return;
        }

        LogEvent le = (LogEvent)msg;
        le.setTimeStamp(new Date(event.timeStamp));

        LogWorker worker = (LogWorker)loggers.get(tid);
        if (null == worker) {
            worker = createLogger(tid);
        }
        worker.append(le);
    }

    public boolean requiresLayout()
    {
        return false;
    }

    public void close()
    {
        shutdownLoggers();
    }

    // private classes --------------------------------------------------------

    private class LogWorker implements Runnable
    {
        private final Tid tid;
        private final MvvmLocalContext mctx = MvvmContextFactory.context();
        private final WeakReference tctxRef;

        private volatile BlockingQueue queue = new ArrayBlockingQueue(QUEUE_SIZE);

        // constructor --------------------------------------------------------

        LogWorker(Tid tid)
        {
            this.tid = tid;

            if (0 == tid.getId()) {
                tctxRef = null;
            } else {
                TransformContext tctx = null == tid ? null
                    : mctx.transformManager().transformContext(tid);
                tctxRef = new WeakReference(tctx);
            }
        }

        // public methods -----------------------------------------------------

        public void append(LogEvent le)
        {
            BlockingQueue q = queue;

            if (null == q) {
                logger.warn("logger is shut down: " + tid);
            } else {
                try {
                    q.put(le);
                } catch (InterruptedException exn) {
                    logger.warn("interrupted", exn);
                }
            }
        }

        public void stop()
        {
            queue = null;
        }

        // Runnable methods ---------------------------------------------------

        public void run()
        {
            List l = new ArrayList(QUEUE_SIZE);

            while (null == tctxRef ? true : null != tctxRef.get()) {
                BlockingQueue q = queue;

                if (null == q) { break; }

                try {
                    drainTo(q, l);
                    persist(l);
                } catch (Exception exn) {
                    logger.warn("danger, will robinson", exn); // never die
                }

                l.clear();
            }

            queue = null;
        }

        // private methods ----------------------------------------------------

        private void drainTo(BlockingQueue q, List l)
        {
            long maxTime = System.currentTimeMillis() + SLEEP_TIME;

            while (l.size() < QUEUE_SIZE) {
                long time = System.currentTimeMillis();

                if (maxTime <= time) { break; }

                try {
                    LogEvent le = (LogEvent)q.poll(maxTime - time,
                                                   TimeUnit.MILLISECONDS);
                    if (null == le) {
                        break;
                    } else {
                        l.add(le);
                    }
                } catch (InterruptedException exn) { continue; }

                q.drainTo(l);
            }
        }

        private void persist(List l)
        {
            logger.debug(tid + " persisting: " + l.size());

            Session s;
            if (null == tctxRef) {
                s = mctx.openSession();
            } else {
                TransformContext tctx = (TransformContext)tctxRef.get();
                if (null == tctx) {
                    logger.warn("transform context no longer exists");
                    return;
                } else {
                    s = tctx.openSession();
                }
            }


            try {
                Transaction tx = s.beginTransaction();

                for (Iterator i = l.iterator(); i.hasNext(); ) {
                    LogEvent le = (LogEvent)i.next();
                    s.save(le);
                }

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn("could persist log events", exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close Hibernate session" , exn);
                }
            }
        }
    }

    private synchronized LogWorker createLogger(Tid tid)
    {
        LogWorker worker = (LogWorker)loggers.get(tid);

        if (null == worker) {
            worker = new LogWorker(tid);
            loggers.put(tid, worker);

            new Thread(worker).start();
        }

        return worker;
    }

    private void shutdownLoggers()
    {
        for (Iterator i = loggers.values().iterator(); i.hasNext(); ) {
            LogWorker worker = (LogWorker)i.next();
            worker.stop();
        }
    }
}
