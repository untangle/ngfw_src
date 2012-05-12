/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.Set;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * <code>PipeSpec</code> for a regular Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class SoloPipeSpec extends PipeSpec
{
    private static final PipelineFoundry FOUNDRY;

    public static final int MIN_STRENGTH = 0;
    public static final int MAX_STRENGTH = 32;

    private final Fitting fitting;
    private final Affinity affinity;
    private final int strength;

    private final Logger logger = Logger.getLogger(getClass());

    private final SessionEventListener listener;
    private ArgonConnector argonConnector;

    // constructors -----------------------------------------------------------

    public SoloPipeSpec(String name, Node node, Set<Subscription> subscriptions,
                        SessionEventListener listener,
                        Fitting fitting, Affinity affinity, int strength)
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

    public SoloPipeSpec(String name, Node node,
                        Subscription subscription,
                        SessionEventListener listener, Fitting fitting,
                        Affinity affinity, int strength)
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

    public SoloPipeSpec(String name, Node node,
                        SessionEventListener listener,
                        Fitting fitting, Affinity affinity,
                        int strength)
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

    public ArgonConnector getArgonConnector()
    {
        return argonConnector;
    }

    // PipeSpec methods -------------------------------------------------------

    @Override
    public void connectArgonConnector()
    {
        if (null == argonConnector) {
            argonConnector = FOUNDRY.createArgonConnector(this, listener);
            FOUNDRY.registerArgonConnector(argonConnector);
        } else {
            logger.warn("argonConnectors already connected");
        }
    }

    @Override
    public void disconnectArgonConnector()
    {
        if (null != argonConnector) {
            FOUNDRY.deregisterArgonConnector(argonConnector);
            argonConnector.destroy();
            argonConnector = null;
        } else {
            logger.warn("argonConnectors not connected");
        }
    }

    @Override
    public List<NodeIPSession> liveSessions()
    {
        if (null != argonConnector) {
            return argonConnector.liveSessions();
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

    // static initialization --------------------------------------------------

    static {
        FOUNDRY = UvmContextFactory.context().pipelineFoundry();
    }
}
