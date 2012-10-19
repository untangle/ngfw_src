/**
 * $Id: CaptureReplacementGenerator.java 31737 2012-04-19 23:13:40Z mahotz $
 */

package com.untangle.node.capture;

import java.net.URI;

import org.apache.log4j.Logger;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;

class CaptureReplacementGenerator extends ReplacementGenerator<CaptureBlockDetails>
{
    private final Logger logger = Logger.getLogger(getClass());
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

    CaptureReplacementGenerator(NodeSettings tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(CaptureBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        logger.debug("getReplacement DETAILS:" + details.toString());

        return String.format(BLOCK_TEMPLATE,
            details.getMethod(),
            details.getHost(),
            details.getUri(),
            uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeSettings nodeSettings)
    {
        CaptureBlockDetails details = getNonceData(nonce);
        logger.debug("getRedirectUrl " + details.toString());
        String retval =  ("http://" + host + "/capture/handler.py/index?nonce=" + nonce);
        retval = (retval + "&method=" + details.getMethod());
        retval = (retval + "&appid=" + nodeSettings.getId());
        retval = (retval + "&host=" + details.getHost());
        retval = (retval + "&uri=" + details.getUri());
        return(retval);
    }
}
