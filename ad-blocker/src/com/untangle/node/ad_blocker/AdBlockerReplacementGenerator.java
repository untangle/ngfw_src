/**
 * $Id$
 */
package com.untangle.node.ad_blocker;

import com.untangle.node.http.BlockDetails;
import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.node.AppSettings;

public class AdBlockerReplacementGenerator extends ReplacementGenerator<BlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "</BODY></HTML>";
    //     private static final String BLOCK_TEMPLATE
    //         = "<HTML><HEAD>"
    //         + "<TITLE>403 Forbidden</TITLE>"
    //         + "</HEAD><BODY>"
    //         + "<p>This site was blocked because it contains ad content</p>"
    //         + "<p>Host: %s</p>"
    //         + "<p>URI: %s</p>"
    //         + "</BODY></HTML>";

    AdBlockerReplacementGenerator(AppSettings tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(BlockDetails details)
    {
        return String.format(BLOCK_TEMPLATE, details.getHost(), details.getUri());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        // no need to redirect; we generate a simple response page
        return null;
    }
}
