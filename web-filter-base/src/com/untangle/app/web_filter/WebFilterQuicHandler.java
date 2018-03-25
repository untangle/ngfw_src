/**
 * $Id: WebFilterQuicHandler.java,v 1.00 2015/11/13 10:33:37 dmorris Exp $
 */
package com.untangle.app.web_filter;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPNewSessionRequest;

public class WebFilterQuicHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase app;

    public WebFilterQuicHandler(WebFilterBase app)
    {
        super(app);

        this.app = app;
    }

    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest request )
    {
        Boolean blockQuic = app.getSettings().getBlockQuic();

        if ( blockQuic == null || !blockQuic ) {
            logger.debug("Ignore QUIC.");
            request.release();
            return;
        }

        logger.info("Block QUIC: " + request);
        request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE );
        return;
    }
}
