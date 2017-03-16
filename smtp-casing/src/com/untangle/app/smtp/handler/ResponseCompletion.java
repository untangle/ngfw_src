package com.untangle.app.smtp.handler;

import com.untangle.app.smtp.Response;
import com.untangle.uvm.vnet.AppTCPSession;

public interface ResponseCompletion
{
    /**
     * Handle a response. The Response is <b>not</b> automatically passed back to the client. If the Completion wishes
     * to pass the Response back through to the client they should use <code>
     * actions.sendResponseToClient(resp);
     * </code> <br>
     * If the Request was synthetic (i.e. issued by the Handler, not the real client) then the response should be
     * supressed. To supress a response from flowing back to the client take no action.
     * 
     * @param resp
     *            the response from Server
     * @param actions
     *            the set of available actions.
     */
    public void handleResponse( AppTCPSession session, Response resp );
}
