/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.net.URI;

import org.apache.log4j.Logger;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

class CaptivePortalReplacementGenerator extends ReplacementGenerator<CaptivePortalBlockDetails>
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp captureNode;

// THIS IS FOR ECLIPSE - @formatter:off
    
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>\r\n"
        + "<TITLE>Captive Portal - Access Denied - Authentication Required</TITLE>\r\n"
        + "</HEAD><BODY>\r\n"
        + "<P><H2><HR><BR><CENTER>This site is blocked because your computer has not been authenticated.</CENTER><BR><HR></H2></P>\r\n"
        + "<P><H3>Request: %s</H3></P>"
        + "<P><H3>Host: %s</H3></P>"
        + "<P><H3>URI: %s</H3></P>"
        + "<P><H3>Please contact %s for assistance.</H3></P>"
        + "</BODY></HTML>";

// THIS IS FOR ECLIPSE - @formatter:on

    CaptivePortalReplacementGenerator(AppSettings nodeId,CaptivePortalApp node)
    {
        super(nodeId);
        this.captureNode = node;
    }

    @Override
    protected String getReplacement(CaptivePortalBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        logger.debug("getReplacement DETAILS:" + details.toString());

        return String.format(BLOCK_TEMPLATE, details.getMethod(), details.getHost(), details.getUri(), uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        CaptivePortalBlockDetails details = getNonceData(nonce);
        logger.debug("getRedirectUrl " + details.toString());
        
        // start with the plaintext prefix for the redirect and switch to secure if the option is enabled
        String prefix = "http://";
        if (captureNode.getCaptivePortalSettings().getAlwaysUseSecureCapture() == true) prefix = "https://";
        
        String retval = (prefix + host + "/capture/handler.py/index?nonce=" + nonce);
        retval = (retval + "&method=" + details.getMethod());
        retval = (retval + "&appid=" + appSettings.getId());
        retval = (retval + "&host=" + details.getHost());
        retval = (retval + "&uri=" + details.getUri());
        return (retval);
    }
}
