/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import org.apache.log4j.Logger;
import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.argon.ArgonAgent;
import com.untangle.uvm.argon.ArgonAgentImpl;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.UDPSession;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * ArgonConnectorImpl is the implementation of a single ArgonConnector.
 * Status and control of a pipe happen here.
 * Events are handled in Dispatcher instead.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class ArgonConnectorImpl implements ArgonConnector
{
    protected ArgonAgent argon;

    private final PipeSpec pipeSpec;

    private boolean lastSessionWriteFailed = false;
    private long lastSessionWriteTime;

    private Dispatcher disp;

    private final Node node;
    private final SessionEventListener listener;

    private final Logger logger;
    private final Logger sessionLogger;
    private final Logger sessionEventLogger;
    private final Logger sessionLoggerTCP;
    private final Logger sessionLoggerUDP;

    // public construction is the easiest solution to access from
    // ArgonConnectorManager for now.
    public ArgonConnectorImpl(PipeSpec pipeSpec, SessionEventListener listener)
    {
        this.node = pipeSpec.getNode();

        this.listener = listener;
        this.pipeSpec = pipeSpec;

        logger = Logger.getLogger(ArgonConnector.class);
        sessionLogger = Logger.getLogger(Session.class);
        sessionEventLogger = Logger.getLogger("com.untangle.uvm.vnet.SessionEvent");
        sessionLoggerTCP = Logger.getLogger(TCPSession.class);
        sessionLoggerUDP = Logger.getLogger(UDPSession.class);

        lastSessionWriteTime = MetaEnv.currentTimeMillis();

        try {
            start();
        } catch (Exception x) {
            logger.error("Exception plumbing ArgonConnector", x);
            destroy();
        }
    }

    public PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    public Node node()
    {
        return node;
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

    public NodeDesc nodeDesc()
    {
        return node().getNodeDesc();
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

    public List<VnetSessionDesc> liveSessionDescs()
    {
        if (disp == null)
            return new LinkedList<VnetSessionDesc>();
        return disp.liveSessionDescs();
    }

    public List<IPSession> liveSessions()
    {
        if (disp != null)
            return disp.liveSessions();
        else
            return null;
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
    
    public long lastSessionWriteTime()
    {
        return lastSessionWriteTime;
    }

    private void start() 
    {
        if (isRunning()) {
            logger.warn("Already running... ignoring start command");
            return;
        }

        disp = new Dispatcher(this);
        if (listener != null)
            disp.setSessionEventListener(listener);
        /* start event loop */
        disp.start();

        argon = new ArgonAgentImpl(pipeSpec.getName(), disp); // Also sets new session listener to dispatcher
    }

    public boolean isRunning()
    {
        return (argon != null && argon.state() == ArgonAgent.LIVE_ARGON);
    }

    /**
     * This is called by the Node (or NodeManager?) to disconnect
     * from a live ArgonConnector. Since it is live we must be sure to shut down the
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
                logger.info("Exception destroying ArgonConnector", x);
            }
            argon = null;
            disp = null;
        }
    }

    public String toString()
    {
        return null == listener ? "no listener" : listener.toString();
    }

    public static ArgonConnector create(PipeSpec pipeSpec, SessionEventListener listener)
    {
        return new ArgonConnectorImpl(pipeSpec, listener);
    }

}
