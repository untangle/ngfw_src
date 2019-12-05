/**
 * $Id: WebFilterQuicHandler.java,v 1.00 2015/11/13 10:33:37 dmorris Exp $
 */

package com.untangle.app.web_filter;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPNewSessionRequest;

/**
 * Handler for QUIC traffic
 */
public class WebFilterQuicHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase app;

    /**
     * Constructor
     * 
     * @param app
     *        The web filter base application
     */
    public WebFilterQuicHandler(WebFilterBase app)
    {
        super(app);

        this.app = app;
    }

    /**
     * Handles new UDP sessions
     * 
     * @param request
     *        The new session request
     */
    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequest request)
    {
        Boolean blockQuic = app.getSettings().getBlockQuic();

        if (blockQuic == null || !blockQuic) {
            logger.debug("Ignore QUIC.");
            request.release();
            return;
        }

        app.incrementQuicBlock();

        Boolean logQuic = app.getSettings().getLogQuic();
        if(logQuic != null && logQuic){
            logger.info("Block QUIC: " + request);
        }
        request.rejectReturnUnreachable(IPNewSessionRequest.PORT_UNREACHABLE);
        return;
    }
}
