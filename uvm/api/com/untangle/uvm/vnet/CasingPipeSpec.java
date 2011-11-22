/*
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.node.token.CasingAdaptor;
import com.untangle.node.token.CasingFactory;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
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

    private final CasingAdaptor insideAdaptor;
    private final CasingAdaptor outsideAdaptor;

    private final Logger logger = Logger.getLogger(getClass());

    private ArgonConnector insideArgonConnector;
    private ArgonConnector outsideArgonConnector;

    private boolean releaseParseExceptions = true;

    // constructors -----------------------------------------------------------

    public CasingPipeSpec(String name, Node node, Set<Subscription> subscriptions, CasingFactory casingFactory, Fitting input, Fitting output)
    {
        super(name, node, subscriptions);

        insideAdaptor = new CasingAdaptor(node, casingFactory, true, true);
        outsideAdaptor = new CasingAdaptor(node, casingFactory, false, true);

        this.input = input;
        this.output = output;
    }

    public CasingPipeSpec(String name, Node node, CasingFactory casingFactory, Fitting input, Fitting output)
    {
        super(name, node);

        insideAdaptor = new CasingAdaptor(node, casingFactory, true, true);
        outsideAdaptor = new CasingAdaptor(node, casingFactory, false, true);

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

    public CasingAdaptor getInsideAdaptor()
    {
        return insideAdaptor;
    }

    public CasingAdaptor getOutsideAdaptor()
    {
        return outsideAdaptor;
    }

    public boolean getReleaseParseExceptions()
    {
        return releaseParseExceptions;
    }

    public void setReleaseParseExceptions(boolean releaseParseExceptions)
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
            insideArgonConnector = FOUNDRY.createArgonConnector(this, insideAdaptor);
            outsideArgonConnector = FOUNDRY.createArgonConnector(this, outsideAdaptor);
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
    public List<VnetSessionDesc> liveSessionDescs()
    {
        List<VnetSessionDesc> l = new ArrayList<VnetSessionDesc>();
        if (null != insideArgonConnector) {
            for (VnetSessionDesc isd : insideArgonConnector.liveSessionDescs()) {
                l.add(isd);
            }
        }

        if (null != outsideArgonConnector) {
            for (VnetSessionDesc isd : outsideArgonConnector.liveSessionDescs()) {
                l.add(isd);
            }
        }

        return l;
    }

    @Override
    public List<IPSession> liveSessions()
    {
        if (null != insideArgonConnector) {
            return insideArgonConnector.liveSessions();
        } else {
            return null;
        }
    }
    
    // static initialization --------------------------------------------------

    static {
        FOUNDRY = LocalUvmContextFactory.context().pipelineFoundry();
    }
}
