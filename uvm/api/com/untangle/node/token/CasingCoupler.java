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
    public CasingCoupler(Node node, Parser parser, Unparser unparser, boolean clientSide, boolean releaseParseExceptions)
    {
        super(node, parser, unparser, clientSide, releaseParseExceptions);
    }

    // SessionEventListener methods -------------------------------------------

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        this.parser.handleNewSession( session );
        this.unparser.handleNewSession( session );
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

        // the casing may have already been shutdown so we only need to
        // call the finalized stuff if it still exists 
        this.parser.handleFinalized( session );
        this.unparser.handleFinalized( session );
    }

    @Override
    public void handleTimer( NodeSession sess )
    {
        this.parser.handleTimer( sess );
    }

    // private methods --------------------------------------------------------

    private void streamParse( NodeTCPSession session, ByteBuffer data, boolean s2c )
    {
        try {
            this.parser.parseFIXME( session, data );
        }
        catch (Exception exn) {
            logger.warn("Error during streamParse()", exn);
            return;
        }

        return;
    }

    private void streamUnparse( NodeTCPSession session, ByteBuffer data, boolean s2c )
    {
        try {
            this.unparser.unparseFIXME( session, data );
        }
        catch (Exception exn) {
            logger.warn("Error during streamUnparse()", exn);
        }

        return;
    }
}
