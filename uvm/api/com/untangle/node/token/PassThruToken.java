/**
 * $Id$
 */
package com.untangle.node.token;


/**
 * Token representing the directive that downstream TokenHandlers
 * should enter pass-through mode (letting through all bytes unmolested).
 */
public class PassThruToken extends MetadataToken
{
    public static final PassThruToken PASSTHRU = new PassThruToken();

    private PassThruToken() {}
}
