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

import java.util.*;

import com.untangle.mvvm.argon.ArgonAgent;
import com.untangle.mvvm.argon.ArgonAgentImpl;
import com.untangle.mvvm.tapi.*;
import com.untangle.mvvm.tapi.event.SessionEventListener;
import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.util.MetaEnv;
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
    private final MPipeManagerImpl xm;
    private final PipeSpec pipeSpec;

    // private SessionFactory sessFact;

    private boolean lastSessionWriteFailed = false;
    private long lastSessionWriteTime;

    private Dispatcher disp;

    private final Transform transform;
    private final SessionEventListener listener;

    // This is the original connection point.  We use it for reconnection.
    // private InetSocketAddress socketAddress;

    private final Logger logger;
    private final Logger sessionLogger;
    private final Logger sessionEventLogger;
    private final Logger sessionLoggerTCP;
    private final Logger sessionLoggerUDP;

    // public construction is the easiest solution to access from
    // MPipeManager for now.
    public MPipeImpl(MPipeManagerImpl xm, PipeSpec pipeSpec,
                     SessionEventListener listener)
    {

        this.xm = xm;
        this.transform = pipeSpec.getTransform();
        this.listener = listener;
        this.pipeSpec = pipeSpec;

        logger = Logger.getLogger(MPipe.class);
        sessionLogger = Logger.getLogger(Session.class);
        // XXX
        sessionEventLogger = Logger.getLogger("com.untangle.mvvm.tapi.SessionEvent");
        sessionLoggerTCP = Logger.getLogger(TCPSession.class);
        sessionLoggerUDP = Logger.getLogger(UDPSession.class);

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

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return null == listener ? "no listener" : listener.toString();
    }
}
