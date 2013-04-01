/**
 * $Id$
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;
import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.netcap.PipelineAgent;
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
    protected PipelineAgent pipelineAgent = null;

    private final PipeSpec pipeSpec;

    private Dispatcher disp;

    private final Node node;
    private final SessionEventListener listener;

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

    public PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    public Node node()
    {
        return node;
    }

    public PipelineAgent getPipelineAgent()
    {
        return pipelineAgent;
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
        if (disp == null)
            return new long[0];
        return disp.liveSessionIds();
    }

    public List<NodeSession> liveSessions()
    {
        if (disp != null)
            return disp.liveSessions();
        else
            return null;
    }
    
    private synchronized  void start() 
    {
        if ( pipelineAgent != null ) {
            logger.warn("Already running... ignoring start command");
            return;
        }

        disp = new Dispatcher(this);
        if (listener != null)
            disp.setSessionEventListener(listener);

        pipelineAgent = new PipelineAgent(pipeSpec.getName(), disp); // Also sets new session listener to dispatcher
    }

    /**
     * This is called by the Node (or NodeManager?) to disconnect
     * from a live PipelineConnector. Since it is live we must be sure to shut down the
     * Dispatcher nicely (in comparison to shutdown, below).
     *
     */
    public synchronized void destroy()
    {
        if ( pipelineAgent != null ) {
            try {
                disp.destroy();
            } catch (Exception x) {
                logger.info("Exception destroying PipelineConnector", x);
            }
            disp = null;

            try {
                pipelineAgent.destroy();
            } catch (Exception x) {
                logger.info("Exception destroying PipelineConnector", x);
            }
            pipelineAgent = null;
        }
    }

    public String toString()
    {
        return null == listener ? "no listener" : listener.toString();
    }
}
