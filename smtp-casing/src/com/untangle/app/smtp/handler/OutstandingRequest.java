package com.untangle.app.smtp.handler;

import java.util.LinkedList;
import java.util.List;

import com.untangle.app.smtp.Response;

public class OutstandingRequest
{
    private final ResponseCompletion responseCompletion;
    private List<Response> additionalActions;

    public OutstandingRequest(ResponseCompletion responseCompletion) {
        this.responseCompletion = responseCompletion;
        additionalActions = new LinkedList<Response>();
    }

    public List<Response> getAdditionalActions()
    {
        return additionalActions;
    }

    public void setAdditionalActions(List<Response> additionalActions)
    {
        this.additionalActions = additionalActions;
    }

    public ResponseCompletion getResponseCompletion()
    {
        return responseCompletion;
    }

}
