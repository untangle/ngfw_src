/**
 * $Id$
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;
import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.argon.ArgonAgent;
import com.untangle.uvm.argon.ArgonAgentImpl;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * ArgonConnectorImpl is the implementation of a single ArgonConnector.
 * Status and control of a pipe happen here.
 * Events are handled in Dispatcher instead.
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
        sessionLogger = Logger.getLogger(NodeSession.class);
        sessionEventLogger = Logger.getLogger(NodeSession.class);
        sessionLoggerTCP = Logger.getLogger(NodeTCPSession.class);
        sessionLoggerUDP = Logger.getLogger(NodeUDPSession.class);

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

    public NodeProperties nodeProperties()
    {
        return node().getNodeProperties();
    }

    public int state()
    {
        return argon.state();
    }

    public long[] liveSessionIds()
    {
        if (disp == null)
            return new long[0];
        return disp.liveSessionIds();
    }

    public List<NodeIPSession> liveSessions()
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
        if ( isRunning() ) {
            try {
                disp.destroy();
            } catch (Exception x) {
                logger.info("Exception destroying ArgonConnector", x);
            }
            disp = null;

            try {
                argon.destroy();
            } catch (Exception x) {
                logger.info("Exception destroying ArgonConnector", x);
            }
            argon = null;
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
