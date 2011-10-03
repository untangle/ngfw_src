/*
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.NodeId;

/**
 * ReplacementGenerator for Virus.
 */
class VirusReplacementGenerator extends ReplacementGenerator<VirusBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    VirusReplacementGenerator(NodeId tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(VirusBlockDetails details)
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getVendor(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeId tid)
    {
        return "http://" + host + "/virus/blockpage?nonce=" + nonce
            + "&tid=" + tid;
    }
}
