/**
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
 */
public class CasingPipeSpec extends PipeSpec
{
    private static final PipelineFoundry FOUNDRY;

    private final Fitting input;
    private final Fitting output;

    private final CasingBase insideAdaptor;
    private final CasingBase outsideAdaptor;

    private final Logger logger = Logger.getLogger(getClass());

    private PipelineConnector insidePipelineConnector;
    private PipelineConnector outsidePipelineConnector;

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
    public void connectPipelineConnector()
    {
        if (null == insidePipelineConnector && null == outsidePipelineConnector) {
            insidePipelineConnector = FOUNDRY.createPipelineConnector(this, insideAdaptor, input, output);
            outsidePipelineConnector = FOUNDRY.createPipelineConnector(this, outsideAdaptor, output, input);
            FOUNDRY.registerCasing(insidePipelineConnector, outsidePipelineConnector);
        } else {
            logger.warn("casing PipelineConnectors already connected");
        }
    }

    @Override
    public void disconnectPipelineConnector()
    {
        if (null != insidePipelineConnector && null != outsidePipelineConnector) {
            FOUNDRY.deregisterCasing(insidePipelineConnector);
            insidePipelineConnector.destroy();
            outsidePipelineConnector.destroy();
            insidePipelineConnector = outsidePipelineConnector = null;
        } else {
            logger.warn("casing PipelineConnectors not connected");
        }
    }

    @Override
    public List<PipelineConnector> getPipelineConnectors()
    {
        ArrayList<PipelineConnector> connectors = new ArrayList<PipelineConnector>();
        connectors.add(insidePipelineConnector);
        connectors.add(outsidePipelineConnector);
        return connectors;
    }

    @Override
    public List<NodeSession> liveSessions()
    {
        if (null != insidePipelineConnector) {
            return insidePipelineConnector.liveSessions();
        } else {
            return null;
        }
    }

    // static initialization --------------------------------------------------

    static {
        FOUNDRY = UvmContextFactory.context().pipelineFoundry();
    }
}
