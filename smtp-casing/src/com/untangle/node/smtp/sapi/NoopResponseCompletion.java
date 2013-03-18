/**
 * $Id$
 */
package com.untangle.node.smtp.sapi;

import com.untangle.node.smtp.Response;

/**
 * Convienence implementation of ResponseCompletion
 * which does nothing
 *
 */
public class NoopResponseCompletion
    implements ResponseCompletion
{
    public void handleResponse(Response resp, Session.SmtpResponseActions actions)
    {
        //Nothing to do...
    }
}
