/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi.impl;

import java.util.*;

import com.metavize.mvvm.argon.ArgonAgent;
import com.metavize.mvvm.argon.ArgonAgentImpl;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.SessionEventListener;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.util.MetaEnv;
import org.apache.log4j.*;


/**
 * MPipeImpl is the implementation of a single MetaPipe.
 * Status and control of a pipe happen here.
 * Events are handled in Dispatcher instead.
 *
 * @author <a href="mailto:jdi@towelie">jdi</a>
 * @version 1.0
 */
class MPipeImpl implements MPipe {

    // Set to true to get lots of debugging output to stdout
    private static final boolean DEBUG = false;

    protected ArgonAgent argon;

    // Our owner/manager/factory
    private MPipeManagerImpl xm;
    private final PipeSpec pipeSpec;

    private SessionEventListener listener;

    // private SessionFactory sessFact;

    private boolean lastSessionWriteFailed = false;
    private long lastSessionWriteTime;

    private Dispatcher disp;

    private Set subscriptions;

    private int        subscriptionId = 1;
    private Object subscriptionIdLock = new Object();

    private Transform transform;

    // This is the original connection point.  We use it for reconnection.
    // private InetSocketAddress socketAddress;

    private Logger logger;
    private Logger sessionLogger;
    private Logger sessionEventLogger;
    private Logger sessionLoggerTCP;
    private Logger sessionLoggerUDP;

    // public construction is the easiest solution to access from
    // MPipeManager for now.
    public MPipeImpl(MPipeManagerImpl xm, Transform transform,
                     PipeSpec pipeSpec)
    {

        this.xm = xm;
        this.transform = transform;
        this.pipeSpec = pipeSpec;

        subscriptions = new HashSet();

        logger = Logger.getLogger(MPipe.class.getName());
        sessionLogger = Logger.getLogger(Session.class.getName());
        // XXX
        sessionEventLogger = Logger.getLogger("com.metavize.mvvm.tapi.SessionEvent");
        sessionLoggerTCP = Logger.getLogger(TCPSession.class.getName());
        sessionLoggerUDP = Logger.getLogger(UDPSession.class.getName());

        lastSessionWriteTime = MetaEnv.currentTimeMillis();

        try {
            start();
        } catch (MPipeException x) {
            logger.error("Exception plumbing MPipe", x);
            destroy();
        }
        // sessFact = new SessionFactoryImpl(this);
    }

    /*
    public void finalize()
    {
        if (comwbuf != null) {
            bufPool.release(comwbuf);
            comwbuf = null;
        }
    }
    */

    public PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    public Transform transform()
    {
        // return argon.transform();
        return transform;
    }

    public ArgonAgent getArgonAgent()
    {
        return argon;
    }

    public Logger logger()
    {
        return logger;
    }

    public Logger sessionLogger()
    {
        return sessionLogger;
    }

    public Logger sessionEventLogger()
    {
        return sessionEventLogger;
    }

    public Logger sessionLoggerTCP()
    {
        return sessionLoggerTCP;
    }

    public Logger sessionLoggerUDP()
    {
        return sessionLoggerUDP;
    }

    public TransformDesc transformDesc()
    {
        return transform().getTransformDesc();
    }

    public int state()
    {
        return argon.state();
    }

    public int[] liveSessionIds()
    {
        if (disp == null)
            return new int[0];
        return disp.liveSessionIds();
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        if (disp == null)
            return new IPSessionDesc[0];
        return disp.liveSessionDescs();
    }

    public void dumpSessions()
    {
        if (disp != null)
            disp.dumpSessions();
    }

    public void lastSessionWriteFailed(boolean failed)
    {
        lastSessionWriteFailed = failed;
        if (!lastSessionWriteFailed)
            lastSessionWriteTime = MetaEnv.currentTimeMillis();
    }

    public boolean lastWriteFailed()
    {
        return (lastSessionWriteFailed);
    }

    /*
    public SessionFactory sessionFactory()
    {

    return sessFact;
    }
    */

    /**
     * A utiliity function to make sure the state is ok for issueing new requests, etc.
     *
     */
    private void checkOk()
        throws MPipeException
    {
        if (!isRunning())
            throw new MPipeException(this, "Attempt to use a MPipe that is not running");
    }


    private void start()
        throws MPipeException
    {
        if (isRunning())
            throw new MPipeException(this, "Attempt to start a MPipe that is already running");

        disp = new Dispatcher(this);
        if (listener != null)
            disp.setSessionEventListener(listener);
        /* start event loop */
        disp.start();

        /* send load cmd */
        /*
          TransformDesc tDesc = transformDesc();
          String name = tDesc.name();
          String signature = Base64.encodeBytes(tDesc.publicKey());
          int position = tDesc.position();
        */

        argon = new ArgonAgentImpl(pipeSpec.getName(), disp); // Also sets new session listener to dispatcher

        // Activate any subscriptions made before mPipe comes up, or
        // re-activate subscriptions with newly restarted mPipe
        remakeSubscriptions();
    }

    public boolean isRunning()
    {
        return (argon != null && argon.state() == ArgonAgent.LIVE_ARGON);
    }

    /**
     * This is called by the Transform (or TransformManager?) to disconnect
     * from a live MPipe. Since it is live we must be sure to shut down the
     * Dispatcher nicely (in comparison to shutdown, below).
     *
     */
    public void destroy()
    {
        if (isRunning()) {
            // Stop the dispatcher and all its threads.
            try {
                disp.destroy(false);
                argon.destroy();
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
            } catch (Exception x) {
                // Not expected, just log
                logger.info("Exception destroying MPipe", x);
            }
            argon = null;
            disp = null;
        }

        xm.destroyed(this);
    }

    private void remakeSubscriptions()
        throws MPipeException
    {
        // Called at MPipe start/restart to sync MPipe subscriptions with internal state
        for (Iterator iter = subscriptions.iterator(); iter.hasNext();) {
            LiveSubscription ls = (LiveSubscription) iter.next();
            doSubAdd(ls.id(), ls.desc());
        }
    }

    public LiveSubscription addSubscription(Subscription desc)
    {
        int subId = getNextSubscriptionID();

        if (isRunning()) {
            /* send subscribe cmd */
            doSubAdd(subId, desc);
        }
        // Otherwise we just wait for shutdown() to remake them when
        // MPipe comes up.

        // On success, go ahead and add the subscription to our set
        // and return it.
        LiveSubscription sub = new LiveSubscription(subId, desc);
        synchronized(subscriptions) {
            subscriptions.add(sub);
        }
        return sub;
    }

    private void doSubAdd(int subId, Subscription sub)
    {
        logger.info("Subscribing " + transform.getTransformDesc().getName()
                    + " to " + sub);

        pipeSpec.addSubscription(sub);
    }

    public void removeSubscription(LiveSubscription toRemove)
    {
        logger.info("Removing Subscription " + transform().getTransformDesc().getName() + " to " + toRemove.desc());
        if (isRunning()) {
            // Mnp req = RequestUtil.createUnsubscribe(toRemove.id());
            // This will throw on timeout or error:
            // request(req, timeoutMillis);
        }

        // On success, go ahead and remove the subscription from our set
        synchronized(subscriptions) {
            subscriptions.remove(toRemove);
        }
    }

    public void clearSubscriptions()
    {
        logger.info("Clearing all Subscriptions");
        synchronized(subscriptions) {
            for (Iterator iter = subscriptions.iterator(); iter.hasNext();) {
                LiveSubscription sub = (LiveSubscription) iter.next();
                removeSubscription(sub);
            }
        }
    }

    private static final LiveSubscription[] subsArrayProto = new LiveSubscription[] { };

    public LiveSubscription[] subscriptions()
    {
        return (LiveSubscription[]) subscriptions.toArray(subsArrayProto);
    }

    public void setSessionEventListener(SessionEventListener listener)
    {
        // Don't really need checkOk here.

        this.listener = listener;
        if (disp != null)
            disp.setSessionEventListener(listener);
    }

    /*
    public void scheduleTimer(IPSessionImpl session, long delay)
    {
        if (disp == null)
            throw new IllegalStateException("MPipe has not been started");
        disp.scheduleTimer(session, delay);
    }

    public void cancelTimer(IPSessionImpl session)
    {
        if (disp != null)
            disp.cancelTimer(session);
    }
    */

    private int getNextSubscriptionID()
    {
        synchronized(subscriptionIdLock) {
            return subscriptionId++;
        }
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return null == listener ? null : listener.toString();
    }
}
