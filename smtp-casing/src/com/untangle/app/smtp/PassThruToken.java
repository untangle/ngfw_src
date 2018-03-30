/**
 * $Id$
 */
package com.untangle.app.smtp;

import com.untangle.uvm.vnet.MetadataToken;

/**
 * Token representing the directive that downstream TokenHandlers
 * should enter pass-through mode (letting through all bytes unmolested).
 */
public class PassThruToken extends MetadataToken
{
    public static final PassThruToken PASSTHRU = new PassThruToken();

    /**
     * Initialize PassThruToken instance.
     * @return PassThruToken instance
     */
    private PassThruToken() {}
}
