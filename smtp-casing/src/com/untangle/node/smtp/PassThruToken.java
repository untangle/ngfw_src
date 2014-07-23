/**
 * $Id$
 */
package com.untangle.node.smtp;

import com.untangle.node.token.MetadataToken;

/**
 * Token representing the directive that downstream TokenHandlers
 * should enter pass-through mode (letting through all bytes unmolested).
 */
public class PassThruToken extends MetadataToken
{
    public static final PassThruToken PASSTHRU = new PassThruToken();

    private PassThruToken() {}
}
