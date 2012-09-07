/*
 * $Id: CaptureReplacementGenerator.java 31737 2012-04-19 23:13:40Z mahotz $
 */

package com.untangle.node.capture;

import org.apache.log4j.Logger;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;

class CaptureReplacementGenerator extends ReplacementGenerator<CaptureBlockDetails>
{
    private final Logger logger = Logger.getLogger(getClass());
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<p>This site is blocked because your computer has not been authenticated.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Please contact %s</p>"
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
                             details.getHost(),
                             details.getUri(),
                             uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeSettings nodeSettings)
    {
        logger.debug("getRedirectUrl NONCE:" + nonce + " HOST:" + host);
        return "http://" + host + "/webui/startPage.do?nonce=" + nonce + "&tid=" + nodeSettings.getId();
    }
}
