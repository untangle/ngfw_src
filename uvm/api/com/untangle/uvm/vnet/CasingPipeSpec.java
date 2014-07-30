/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;

/**
 * <code>PipeSpec</code> for a <code>Casing</code>.
 */
public class CasingPipeSpec extends PipeSpec
{
    private final Fitting input;
    private final Fitting output;

    private final SessionEventHandler insideAdaptor;
    private final SessionEventHandler outsideAdaptor;

    private final Logger logger = Logger.getLogger(getClass());

    private PipelineConnector insidePipelineConnector;
    private PipelineConnector outsidePipelineConnector;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Node node, Set<Subscription> subscriptions, SessionEventHandler clientHandler, SessionEventHandler serverHandler, Fitting input, Fitting output)
    {
        super(name, node, subscriptions);

        this.insideAdaptor = clientHandler;
        this.outsideAdaptor = serverHandler;

        this.input = input;
        this.output = output;
    }

    public CasingPipeSpec(String name, Node node, SessionEventHandler clientHandler, SessionEventHandler serverHandler, Fitting input, Fitting output)
    {
        super(name, node);

        this.insideAdaptor = clientHandler;
        this.outsideAdaptor = serverHandler;

        this.input = input;
        this.output = output;
    }
    
    @Override
    public void connectPipelineConnector()
    {
        if (null == insidePipelineConnector && null == outsidePipelineConnector) {
            insidePipelineConnector = UvmContextFactory.context().pipelineFoundry().createPipelineConnector(this, insideAdaptor, input, output);
            outsidePipelineConnector = UvmContextFactory.context().pipelineFoundry().createPipelineConnector(this, outsideAdaptor, output, input);
            UvmContextFactory.context().pipelineFoundry().registerCasing(insidePipelineConnector, outsidePipelineConnector);
        } else {
            logger.warn("casing PipelineConnectors already connected");
        }
    }

    @Override
    public void disconnectPipelineConnector()
    {
        if (null != insidePipelineConnector && null != outsidePipelineConnector) {
            UvmContextFactory.context().pipelineFoundry().deregisterCasing(insidePipelineConnector);
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
}
