/**
 * $Id$
 */
package com.untangle.node.smtp.sapi;

import com.untangle.node.smtp.Response;

/**
 * Callback interface for Object wishing to know
 * a Response has returned from the server.
 * ResponseCompletion instances are associated
 * with a Response via the handleXXXXXXX methods
 * on
 * {@link com.untangle.node.smtp.sapi.SessionHandler SessionHandler}
 * and
 * {@link com.untangle.node.smtp.sapi.TransactionHandler TransactionHandler}
 * <br>
 * The original Command is <b>not</b> passed into the callback method
 * on this interface.  Instances of ResponseCompletion which need to
 * know with which Command they are associated should be constructed
 * to "remember" this.
 */
public interface ResponseCompletion
{
    /**
     * Handle a response.  The Response is <b>not</b>
     * automatically passed back to the client.  If the
     * Completion wishes to pass the Response back
     * through to the client they should use
     * <code>
     * actions.sendResponseToClient(resp);
     * </code>
     * <br>
     * If the Request was synthetic (i.e. issued by the Handler,
     * not the real client) then the response should be supressed.
     * To supress a response from flowing back to the client
     * take no action.
     *
     * @param resp the response from Server
     * @param actions the set of available actions.
     */
    public void handleResponse(Response resp, Session.SmtpResponseActions actions);

}
