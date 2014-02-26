/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import org.apache.log4j.Logger;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * PipelineConnectorImpl is the implementation of a single PipelineConnector.
 * Status and control of a pipe happen here.
 * Events are handled in Dispatcher instead.
 */
public class PipelineConnectorImpl implements PipelineConnector
{
    /**
     * Live flag
     */
    private boolean live = false;
    
    /**
     * Active Sessions for this agent
     */
    private Set<NodeSession> activeSessions = new HashSet<NodeSession>();

    private final PipeSpec pipeSpec;

    private Dispatcher dispatcher;
    private SessionEventListener listener;

    private final Node node;

    private final Logger logger;
    private final Logger sessionLogger;
    private final Logger sessionEventLogger;
    private final Logger sessionLoggerTCP;
    private final Logger sessionLoggerUDP;

    private final Fitting inputFitting;
    private final Fitting outputFitting;
    
    // public construction is the easiest solution to access from
    // PipelineConnectorManager for now.
    public PipelineConnectorImpl(PipeSpec pipeSpec, SessionEventListener listener, Fitting inputFitting, Fitting outputFitting )
    {
        this.node = pipeSpec.getNode();

        this.listener = listener;
        this.pipeSpec = pipeSpec;
        this.dispatcher = dispatcher;

        logger = Logger.getLogger(PipelineConnector.class);
        sessionLogger = Logger.getLogger(NodeSession.class);
        sessionEventLogger = Logger.getLogger(NodeSession.class);
        sessionLoggerTCP = Logger.getLogger(NodeTCPSession.class);
        sessionLoggerUDP = Logger.getLogger(NodeUDPSession.class);
        this.inputFitting = inputFitting;
        this.outputFitting = outputFitting;
        
        try {
            start();
        } catch (Exception x) {
            logger.error("Exception plumbing PipelineConnector", x);
            destroy();
        }
    }

    public Dispatcher getDispatcher()
    {
        return dispatcher;
    }

    public PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    public Node node()
    {
        return node;
    }

    public Fitting getInputFitting()
    {
        return inputFitting;
    }

    public Fitting getOutputFitting()
    {
        return outputFitting;
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

    public long[] liveSessionIds()
    {
        if (dispatcher == null)
            return new long[0];
        return dispatcher.liveSessionIds();
    }

    public List<NodeSession> liveSessions()
    {
        if (dispatcher != null)
            return dispatcher.liveSessions();
        else
            return null;
    }
    
    private synchronized  void start() 
    {
        if ( this.live )
            return;
        
        dispatcher = new Dispatcher(this);

        if (listener != null)
            dispatcher.setSessionEventListener( listener );

        this.live = true;
    }

    /**
     * This is called by the Node (or NodeManager?) to disconnect
     * from a live PipelineConnector. Since it is live we must be sure to shut down the
     * Dispatcher nicely (in comparison to shutdown, below).
     *
     */
    public synchronized void destroy()
    {
        if ( ! this.live ) return;

        try {
            dispatcher.destroy();
        } catch (Exception x) {
            logger.info("Exception destroying PipelineConnector", x);
        }
        dispatcher = null;

        SessionTable.getInstance().shutdownActive();

        /* Remove all of the active sessions */
        activeSessions.clear();

        this.live = false;
    }


    /**
     * Add a session to the map of active sessions.
     * @return True if the session was added, false if the agent is dead, or the session
     *   has already been added.
     */
    public synchronized boolean addSession( NodeSession session )
    {
        return activeSessions.add( session );
    }

    /**
     * Remove a session from the map of active sessions associated with this netcap agent.
     * @return True if the session was removed, false if the session was not in the list 
     *   of active session.
     */
    public synchronized boolean removeSession( NodeSession session )
    {
        return activeSessions.remove( session );
    }
    
    public String toString()
    {
        return "PipelineConnector[" + this.pipeSpec.getName() + "]";
    }
}
