/**
 * $Id$
 */
package com.untangle.app.ad_blocker;

import com.untangle.app.http.BlockDetails;
import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.app.AppSettings;

/**
 * The HTML Replacement generator used to generate the
 * response to a request blocked by Ad Blocker
 */
public class AdBlockerReplacementGenerator extends ReplacementGenerator<BlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "</BODY></HTML>";

    /**
     * Generate a new AdBlockerReplacementGenerator
     * @param appSettings the AppSettings
     */
    protected AdBlockerReplacementGenerator(AppSettings appSettings)
    {
        super(appSettings);
    }

    /**
     * Get the HTML replacement for the specified block
     * @param details The block details
     * @return The HTML response
     */
    @Override
    protected String getReplacement(BlockDetails details)
    {
        return String.format(BLOCK_TEMPLATE, details.getHost(), details.getUri());
    }

    /**
     * Get the redirect URL for the this block
     * Ad Blocker does not use redirects so this just returns null
     *
     * @param nonce The nonce
     * @param host The host
     * @param appSettings the AppSettings
     * @return null
     */
    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        // no need to redirect; we generate a simple response page
        return null;
    }
}
