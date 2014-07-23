/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.node.token.CasingCoupler;
import com.untangle.node.token.CasingAdaptor;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
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

    private final SessionEventListener insideAdaptor;
    private final SessionEventListener outsideAdaptor;

    private final Logger logger = Logger.getLogger(getClass());

    private PipelineConnector insidePipelineConnector;
    private PipelineConnector outsidePipelineConnector;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Node node, Set<Subscription> subscriptions, Parser c2sParser, Parser s2cParser, Unparser c2sUnparser, Unparser s2cUnparser, Fitting input, Fitting output)
    {
        super(name, node, subscriptions);

        switch ( output ) {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, c2sParser, s2cUnparser, true, true);
            outsideAdaptor = new CasingCoupler(node, s2cParser, c2sUnparser, false, true);
            break;
        default:
            insideAdaptor = new CasingAdaptor(node, c2sParser, s2cUnparser, true, true);
            outsideAdaptor = new CasingAdaptor(node, s2cParser, c2sUnparser, false, true);
        }

        this.input = input;
        this.output = output;
    }

    public CasingPipeSpec(String name, Node node, Set<Subscription> subscriptions, Parser parser, Unparser unparser, Fitting input, Fitting output)
    {
        super(name, node, subscriptions);

        switch ( output ) {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, parser, unparser, true, true);
            outsideAdaptor = new CasingCoupler(node, parser, unparser, false, true);
            break;
        default:
            insideAdaptor = new CasingAdaptor(node, parser, unparser, true, true);
            outsideAdaptor = new CasingAdaptor(node, parser, unparser, false, true);
        }

        this.input = input;
        this.output = output;
    }

    public CasingPipeSpec(String name, Node node, Parser c2sParser, Parser s2cParser, Unparser c2sUnparser, Unparser s2cUnparser, Fitting input, Fitting output)
    {
        super(name, node);

        switch ( output ) {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, c2sParser, s2cUnparser, true, true);
            outsideAdaptor = new CasingCoupler(node, s2cParser, c2sUnparser, false, true);
            break;
        default:
            insideAdaptor = new CasingAdaptor(node, c2sParser, s2cUnparser, true, true);
            outsideAdaptor = new CasingAdaptor(node, s2cParser, c2sUnparser, false, true);
        }

        this.input = input;
        this.output = output;
    }
    
    public CasingPipeSpec(String name, Node node, Parser parser, Unparser unparser, Fitting input, Fitting output)
    {
        super(name, node);

        switch ( output ) {
        case HTTP_STREAM:
            insideAdaptor = new CasingCoupler(node, parser, unparser, true, true);
            outsideAdaptor = new CasingCoupler(node, parser, unparser, false, true);
            break;
        default:
            insideAdaptor = new CasingAdaptor(node, parser, unparser, true, true);
            outsideAdaptor = new CasingAdaptor(node, parser, unparser, false, true);
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

    public SessionEventListener getInsideAdaptor()
    {
        return insideAdaptor;
    }

    public SessionEventListener getOutsideAdaptor()
    {
        return outsideAdaptor;
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
