/**
 * $Id: CasingCoupler.java 34281 2013-03-28 00:00:00Z mahotz $
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;

public class CasingCoupler extends CasingBase
{
    public CasingCoupler(Node node, CasingFactory casingFactory, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node,casingFactory,clientSide,releaseParseExceptions);
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
    {
        NodeTCPSession session = e.session();

        Casing casing = casingFactory.casing( session, clientSide );
        Pipeline pipeline = pipeFoundry.getPipeline( session.id() );

        if (logger.isDebugEnabled()) {
            logger.debug("new session setting: " + pipeline + " for: " + session.id());
        }

        addCasing( session, casing, pipeline );
    }

    @Override
    public IPDataResult handleTCPClientChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        Casing casing = casingFactory.casing( e.session(), clientSide );

        if (clientSide) return streamParse(casing, e, false);
        else return streamUnparse(casing, e, false);
    }

    @Override
    public IPDataResult handleTCPServerChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        Casing casing = casingFactory.casing( e.session(), clientSide );

        if (clientSide) return streamUnparse(casing, e, true);
        else return streamParse(casing, e, true);
    }

    @Override
    public IPDataResult handleTCPClientDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        Casing casing = casingFactory.casing( e.session(), clientSide );

        if (clientSide) return streamParse(casing, e, false);
        else return streamUnparse(casing, e, false);
    }

    @Override
    public IPDataResult handleTCPServerDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        Casing casing = casingFactory.casing( e.session(), clientSide );

        if (clientSide) return streamUnparse(casing, e, true);
        else return streamParse(casing, e, true);
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + e.session().id());
        }
        Casing c = getCasing(e.ipsession());
        c.parser().handleFinalized();
        c.unparser().handleFinalized();
        removeCasingDesc(e.session());
    }

    @Override
    public void handleTimer(IPSessionEvent e)
    {
        NodeTCPSession s = (NodeTCPSession)e.ipsession();

        Parser p = getCasing(s).parser();
        p.handleTimer();
        // XXX unparser doesnt get one, does it need it?
    }

    // private methods --------------------------------------------------------

    private IPDataResult streamParse(Casing casing, TCPChunkEvent e, boolean s2c)
    {
        Parser parser = casing.parser();
        TCPChunkResult result = null;
        
        try
        {
            result = parser.parse(e);
        }

        catch (Exception exn)
        {
            logger.warn("Error during streamParse()", exn);
            return null;
        }

        return(result);
    }

    private IPDataResult streamUnparse(Casing casing, TCPChunkEvent e, boolean s2c)
    {
        Unparser unparser = casing.unparser();
        TCPChunkResult result = null;

        try
        {
            result = unparser.unparse(e);
        }

        catch (Exception exn)
        {
            logger.warn("Error during streamUnparse()", exn);
            return null;
        }

        return(result);
    }
}
