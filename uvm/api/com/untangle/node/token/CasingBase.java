/**
 * $Id: CasingAdaptor.java 34281 2013-03-16 00:16:13Z dmorris $
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.NodeSession;

/**
 * Adapts a Token session's underlying byte-stream a <code>Casing</code>.
 */
public class CasingBase extends AbstractEventHandler
{
    protected final CasingFactory casingFactory;
    protected final boolean clientSide;
    protected volatile boolean releaseParseExceptions;

    protected final Map<NodeSession,CasingDesc> casings = new ConcurrentHashMap<NodeSession,CasingDesc>();
    protected final PipelineFoundry pipeFoundry = UvmContextFactory.context().pipelineFoundry();
    protected final Logger logger = Logger.getLogger(CasingBase.class);

    public CasingBase(Node node, CasingFactory casingFactory, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node);
        this.casingFactory = casingFactory;
        this.clientSide = clientSide;
        this.releaseParseExceptions = releaseParseExceptions;
    }

    // accessors --------------------------------------------------------------

    public boolean getReleaseParseExceptions()
    {
        return releaseParseExceptions;
    }

    public void setReleaseParseExceptions(boolean releaseParseExceptions)
    {
        this.releaseParseExceptions = releaseParseExceptions;
    }

    // CasingDesc utils -------------------------------------------------------

    protected static class CasingDesc
    {
        final Casing casing;
        final Pipeline pipeline;

        CasingDesc(Casing casing, Pipeline pipeline)
        {
            this.casing = casing;
            this.pipeline = pipeline;
        }
    }

    protected void addCasing(NodeSession session, Casing casing, Pipeline pipeline)
    {
        casings.put(session, new CasingDesc(casing, pipeline));
    }

    protected CasingDesc getCasingDesc(NodeSession session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc;
    }

    protected Casing getCasing(NodeSession session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc.casing;
    }

    protected Pipeline getPipeline(NodeSession session)
    {
        CasingDesc casingDesc = casings.get(session);
        return casingDesc.pipeline;
    }

    protected void removeCasingDesc(NodeSession session)
    {
        casings.remove(session);
    }
}
