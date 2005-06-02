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
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = 1000;
    private static final int SLEEP_TIME = 3000;

    private static final Logger logger = Logger
        .getLogger(HibernateAppender.class);

    private final Map<Tid, LogWorker> loggers;

    // constructors -----------------------------------------------------------

    public HibernateAppender()
    {
        loggers = new ConcurrentHashMap<Tid, LogWorker>();

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

        LogWorker worker = loggers.get(tid);
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
        private final BlockingQueue<LogEvent> queue;
        private final MvvmLocalContext mctx = MvvmContextFactory.context();
        private final WeakReference<TransformContext> tctxRef;

        private volatile Thread thread;

        // constructor --------------------------------------------------------

        LogWorker(Tid tid)
        {
            this.tid = tid;
            this.queue = new ArrayBlockingQueue<LogEvent>(QUEUE_SIZE);

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
            if (null == thread) {
                logger.warn("logger is shut down: " + tid);
            } else {
                if (!queue.offer(le)) {
                    logger.warn("dropped log event: " + le);
                }
            }
        }

        public void stop()
        {
            Thread t = thread;
            thread = null;
            t.interrupt();
        }

        // Runnable methods ---------------------------------------------------

        public void run()
        {
            List<LogEvent> l = new ArrayList<LogEvent>(BATCH_SIZE);

            thread = Thread.currentThread();

            while (null != thread
                   && null == tctxRef ? true : null != tctxRef.get()) {
                try {
                    drainTo(l);
                    if (null != thread) {
                        persist(l);
                    }
                } catch (Exception exn) {
                    logger.warn("danger, will robinson", exn); // never die
                }

                l.clear();
            }
        }

        // private methods ----------------------------------------------------

        private void drainTo(List<LogEvent> l)
        {
            long maxTime = System.currentTimeMillis() + SLEEP_TIME;

            while (null != thread && l.size() < BATCH_SIZE) {
                long time = System.currentTimeMillis();

                if (maxTime <= time) { break; }
                
                try {
                    LogEvent le = (LogEvent)queue.poll(maxTime - time,
                                                       TimeUnit.MILLISECONDS);
                    if (null == le) {
                        break;
                    } else {
                        l.add(le);
                    }
                } catch (InterruptedException exn) { continue; }

                queue.drainTo(l, BATCH_SIZE - l.size());
            }
        }

        private void persist(List<LogEvent> l)
        {
            logger.debug(tid + " persisting: " + l.size());

            Session s;
            if (null == tctxRef) {
                s = mctx.openSession();
            } else {
                TransformContext tctx = tctxRef.get();
                if (null == tctx) {
                    logger.warn("transform context no longer exists");
                    return;
                } else {
                    s = tctx.openSession();
                }
            }


            try {
                Transaction tx = s.beginTransaction();

                for (LogEvent logEvent : l) {
                    s.save(logEvent);
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
        LogWorker worker = loggers.get(tid);

        if (null == worker) {
            worker = new LogWorker(tid);
            loggers.put(tid, worker);

            new Thread(worker).start();
        }

        return worker;
    }

    private void shutdownLoggers()
    {
        for (LogWorker worker : loggers.values()) {
            worker.stop();
        }
    }
}
