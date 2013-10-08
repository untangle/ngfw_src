/*
 * $Id: SoloPipeSpec.java 34443 2013-04-01 22:53:15Z dmorris $
 */
package com.untangle.uvm.vnet;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * <code>PipeSpec</code> for a regular Node.
 */
public class SoloPipeSpec extends PipeSpec
{
    public static final int MIN_STRENGTH = 0;
    public static final int MAX_STRENGTH = 32;

    private final Logger logger = Logger.getLogger(getClass());

    private final Fitting fitting;
    private final Affinity affinity;
    private final int strength;

    private final SessionEventListener listener;
    private PipelineConnector pipelineConnector;

    // constructors -----------------------------------------------------------

    public SoloPipeSpec(String name, Node node, Set<Subscription> subscriptions, SessionEventListener listener, Fitting fitting, Affinity affinity, int strength)
    {
        super(name, node, subscriptions);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Node node, Subscription subscription, SessionEventListener listener, Fitting fitting, Affinity affinity, int strength)
    {
        super(name, node, subscription);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Node node, SessionEventListener listener, Fitting fitting, Affinity affinity, int strength)
    {
        super(name, node);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    // accessors --------------------------------------------------------------

    public SessionEventListener getListener()
    {
        return listener;
    }

    public Fitting getFitting()
    {
        return fitting;
    }

    public Affinity getAffinity()
    {
        return affinity;
    }

    public int getStrength()
    {
        return strength;
    }

    public PipelineConnector getPipelineConnector()
    {
        return pipelineConnector;
    }

    // PipeSpec methods -------------------------------------------------------

    @Override
    public void connectPipelineConnector()
    {
        if (null == pipelineConnector) {
            this.pipelineConnector = UvmContextFactory.context().pipelineFoundry().createPipelineConnector( this, listener, fitting, fitting );
            UvmContextFactory.context().pipelineFoundry().registerPipelineConnector(pipelineConnector);
        } else {
            logger.warn("pipelineConnectors already connected");
        }
    }

    @Override
    public void disconnectPipelineConnector()
    {
        if (null != pipelineConnector) {
            UvmContextFactory.context().pipelineFoundry().deregisterPipelineConnector(pipelineConnector);
            pipelineConnector.destroy();
            pipelineConnector = null;
        } else {
            logger.warn("pipelineConnectors not connected");
        }
    }

    @Override
    public List<PipelineConnector> getPipelineConnectors()
    {
        ArrayList<PipelineConnector> connectors = new ArrayList<PipelineConnector>();
        connectors.add(pipelineConnector);
        return connectors;
    }
    
    @Override
    public List<NodeSession> liveSessions()
    {
        if (null != pipelineConnector) {
            return pipelineConnector.liveSessions();
        } else {
            return null;
        }
    }
    
    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        return "[SoloPipeSpec A: " + affinity + " S: " + strength + "]";
    }
}
