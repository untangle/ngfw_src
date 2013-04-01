/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.node.token.CasingBase;
import com.untangle.node.token.CasingCoupler;
import com.untangle.node.token.CasingAdaptor;
import com.untangle.node.token.CasingFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;

/**
 * <code>PipeSpec</code> for a <code>Casing</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class CasingPipeSpec extends PipeSpec
{
    private static final PipelineFoundry FOUNDRY;

    private final Fitting input;
    private final Fitting output;

    private final CasingBase insideAdaptor;
    private final CasingBase outsideAdaptor;

    private final Logger logger = Logger.getLogger(getClass());

    private ArgonConnector insideArgonConnector;
    private ArgonConnector outsideArgonConnector;

    private boolean releaseParseExceptions = true;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Node node, Set<Subscription> subscriptions, CasingFactory casingFactory, Fitting input, Fitting output)
    {
        super(name, node, subscriptions);

        switch(output)
        {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, casingFactory, true, true);
            outsideAdaptor = new CasingCoupler(node, casingFactory, false, true);
            break;

        default:
            insideAdaptor = new CasingAdaptor(node, casingFactory, true, true);
            outsideAdaptor = new CasingAdaptor(node, casingFactory, false, true);
        }

        this.input = input;
        this.output = output;
    }

    public CasingPipeSpec(String name, Node node, CasingFactory casingFactory, Fitting input, Fitting output)
    {
        super(name, node);

        switch(output)
        {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, casingFactory, true, true);
            outsideAdaptor = new CasingCoupler(node, casingFactory, false, true);
            break;

        default:
            insideAdaptor = new CasingAdaptor(node, casingFactory, true, true);
            outsideAdaptor = new CasingAdaptor(node, casingFactory, false, true);
        }

        this.input = input;
        this.output = output;
    }

    // accessors --------------------------------------------------------------

    public Fitting getInput()
    {
        return input;
    }

    public Fitting getOutput()
    {
        return output;
    }

    public CasingBase getInsideAdaptor()
    {
        return insideAdaptor;
    }

    public CasingBase getOutsideAdaptor()
    {
        return outsideAdaptor;
    }

    public boolean getReleaseParseExceptions()
    {
        return releaseParseExceptions;
    }

    public void setReleaseParseExceptions( boolean releaseParseExceptions )
    {
        this.releaseParseExceptions = releaseParseExceptions;
        insideAdaptor.setReleaseParseExceptions(releaseParseExceptions);
        outsideAdaptor.setReleaseParseExceptions(releaseParseExceptions);
    }

    // PipeSpec methods -------------------------------------------------------

    @Override
    public void connectArgonConnector()
    {
        if (null == insideArgonConnector && null == outsideArgonConnector) {
            insideArgonConnector = FOUNDRY.createArgonConnector(this, insideAdaptor, input, output);
            outsideArgonConnector = FOUNDRY.createArgonConnector(this, outsideAdaptor, output, input);
            FOUNDRY.registerCasing(insideArgonConnector, outsideArgonConnector);
        } else {
            logger.warn("casing ArgonConnectors already connected");
        }
    }

    @Override
    public void disconnectArgonConnector()
    {
        if (null != insideArgonConnector && null != outsideArgonConnector) {
            FOUNDRY.deregisterCasing(insideArgonConnector);
            insideArgonConnector.destroy();
            outsideArgonConnector.destroy();
            insideArgonConnector = outsideArgonConnector = null;
        } else {
            logger.warn("casing ArgonConnectors not connected");
        }
    }

    @Override
    public List<ArgonConnector> getArgonConnectors()
    {
        ArrayList<ArgonConnector> connectors = new ArrayList<ArgonConnector>();
        connectors.add(insideArgonConnector);
        connectors.add(outsideArgonConnector);
        return connectors;
    }

    @Override
    public List<NodeSession> liveSessions()
    {
        if (null != insideArgonConnector) {
            return insideArgonConnector.liveSessions();
        } else {
            return null;
        }
    }

    // static initialization --------------------------------------------------

    static {
        FOUNDRY = UvmContextFactory.context().pipelineFoundry();
    }
}
