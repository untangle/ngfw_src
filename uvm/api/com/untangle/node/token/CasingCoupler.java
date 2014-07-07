/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
//import com.untangle.uvm.vnet.Pipeline;
//import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;

public class CasingCoupler extends CasingBase
{
    public CasingCoupler(Node node, CasingFactory casingFactory, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node, casingFactory, clientSide, releaseParseExceptions);
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession(TCPSessionEvent e)
    {
        NodeTCPSession session = e.session();

        Casing casing = casingFactory.casing(session, clientSide);
        //Pipeline pipeline = pipeFoundry.getPipeline(session.id());

        // if (logger.isDebugEnabled()) {
        //     logger.debug("new session setting: " + pipeline + " for: " + session.id());
        // }

        session.attach( casing );
        
        //addCasing(session, casing, pipeline);
    }

    @Override
    public void handleTCPClientChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide)
            streamParse(e, false);
        else
            streamUnparse(e, false);
        return;
    }

    @Override
    public void handleTCPServerChunk(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide)
            streamUnparse(e, true);
        else
            streamParse(e, true);
        return;
    }

    @Override
    public void handleTCPClientDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + e.session().id());
        }

        if (clientSide) 
            streamParse(e, false);
        else
            streamUnparse(e, false);
        return;
    }

    @Override
    public void handleTCPServerDataEnd(TCPChunkEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + e.session().id());
        }

        if (clientSide)
            streamUnparse(e, true);
        else
            streamParse(e, true);
        return;
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent e)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + e.session().id());
        }
        Casing c = (Casing) e.session().attachment();

        // the casing may have already been shutdown so we only need to
        // call the finalized stuff if it still exists 
        if (c != null) {
            c.parser().handleFinalized();
            c.unparser().handleFinalized();
        }

        //removeCasingDesc(e.session());
    }

    @Override
    public void handleTimer( NodeSession sess )
    {
        Casing c = (Casing) sess.attachment();
        Parser p = c.parser();
        p.handleTimer();
        // XXX unparser doesnt get one, does it need it?
    }

    // private methods --------------------------------------------------------

    private void streamParse(TCPChunkEvent e, boolean s2c)
    {
        NodeTCPSession session = e.session();
        Casing casing = (Casing) e.session().attachment();
        Parser parser = casing.parser();

        try {
            parser.parse(e);
        }

        catch (Exception exn) {
            logger.warn("Error during streamParse()", exn);
            return;
        }

        return;
    }

    private void streamUnparse(TCPChunkEvent e, boolean s2c)
    {
        NodeTCPSession session = e.session();
        Casing casing = (Casing) e.session().attachment();
        Unparser unparser = casing.unparser();

        try {
            unparser.unparse(e);
        }

        catch (Exception exn) {
            logger.warn("Error during streamUnparse()", exn);
        }

        return;
    }
}
