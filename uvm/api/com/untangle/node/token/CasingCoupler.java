/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

public class CasingCoupler extends CasingBase
{
    public CasingCoupler(Node node, CasingFactory casingFactory, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node, casingFactory, clientSide, releaseParseExceptions);
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        Casing casing = casingFactory.casing(session, clientSide);
        //Pipeline pipeline = pipeFoundry.getPipeline(session.id());

        // if (logger.isDebugEnabled()) {
        //     logger.debug("new session setting: " + pipeline + " for: " + session.id());
        // }

        session.attach( casing );
        
        //addCasing(session, casing, pipeline);
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + session.id());
        }

        if (clientSide)
            streamParse( session, data, false );
        else
            streamUnparse( session, data, false );
        return;
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + session.id());
        }

        if (clientSide)
            streamUnparse( session, data, true );
        else
            streamParse( session, data, true );
        return;
    }

    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling client chunk, session: " + session.id());
        }

        if (clientSide) 
            streamParse( session, data, false );
        else
            streamUnparse( session, data, false );
        return;
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handling server chunk, session: " + session.id());
        }

        if (clientSide)
            streamUnparse( session, data, true );
        else
            streamParse( session, data, true);
        return;
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("finalizing " + session.id());
        }
        Casing c = (Casing) session.attachment();

        // the casing may have already been shutdown so we only need to
        // call the finalized stuff if it still exists 
        if (c != null) {
            c.parser().handleFinalized();
            c.unparser().handleFinalized();
        }

        //removeCasingDesc(session);
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

    private void streamParse( NodeTCPSession session, ByteBuffer data, boolean s2c )
    {
        Casing casing = (Casing) session.attachment();
        Parser parser = casing.parser();

        try {
            parser.parse( session, data );
        }

        catch (Exception exn) {
            logger.warn("Error during streamParse()", exn);
            return;
        }

        return;
    }

    private void streamUnparse( NodeTCPSession session, ByteBuffer data, boolean s2c )
    {
        Casing casing = (Casing) session.attachment();
        Unparser unparser = casing.unparser();

        try {
            unparser.unparse( session, data );
        }

        catch (Exception exn) {
            logger.warn("Error during streamUnparse()", exn);
        }

        return;
    }
}
