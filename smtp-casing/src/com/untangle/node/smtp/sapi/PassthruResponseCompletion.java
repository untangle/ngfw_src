/**
 * $Id$
 */
package com.untangle.node.smtp.sapi;

import com.untangle.node.smtp.Response;

/**
 * Convienence implementation of ResponseCompletion
 * which simply passes back to the client.
 *
 */
public class PassthruResponseCompletion implements ResponseCompletion
{
    public void handleResponse(Response resp, Session.SmtpResponseActions actions)
    {
        actions.sendResponseToClient(resp);
    }
}
