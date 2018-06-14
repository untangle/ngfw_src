/**
 * $Id$
 */
package com.untangle.app.smtp.handler;

import java.util.LinkedList;
import java.util.List;

import com.untangle.app.smtp.Response;

/**
 * Handle outstanding requests
 */
public class OutstandingRequest
{
    private final ResponseCompletion responseCompletion;
    private List<Response> additionalActions;

    /**
     * Initialize intance of OutstandingRequest.
     * @param  responseCompletion ResponseCompletion to set in request.
     * @return                    instance of OutstandingRequest.
     */
    public OutstandingRequest(ResponseCompletion responseCompletion) {
        this.responseCompletion = responseCompletion;
        additionalActions = new LinkedList<Response>();
    }

    /**
     * Return additional actons.
     * @return List of Response.
     */
    public List<Response> getAdditionalActions()
    {
        return additionalActions;
    }

    /**
     * Write additional actions.
     * @param additionalActions List of Responses to set.
     */
    public void setAdditionalActions(List<Response> additionalActions)
    {
        this.additionalActions = additionalActions;
    }

    /**
     * Return current response completion.
     * @return Current response completion.
     */
    public ResponseCompletion getResponseCompletion()
    {
        return responseCompletion;
    }

}
