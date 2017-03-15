/**
 * $Id: WebFilterQuicHandler.java,v 1.00 2015/11/13 10:33:37 dmorris Exp $
 */
package com.untangle.node.web_filter;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.HttpRequestEvent;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.IPNewSessionRequest;

public class WebFilterQuicHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase node;

    public WebFilterQuicHandler(WebFilterBase node)
    {
        super(node);

        this.node = node;
    }

    @Override
    public void handleUDPNewSessionRequest( UDPNewSessionRequest request )
    {
        Boolean blockQuic = node.getSettings().getBlockQuic();

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
