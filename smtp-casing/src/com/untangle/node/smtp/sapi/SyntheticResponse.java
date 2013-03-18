/**
 * $Id$
 */
package com.untangle.node.smtp.sapi;

/**
 * A SyntheticResponse is used when a Synthetic Response (@see SessionHandler)
 * must be issued.  The SyntheticResponse is {@link #handle invoked}
 * by the session when it is appropriate (based on outstanding request
 * ordering) to issue a Response to the client.
 */
public interface SyntheticResponse
{
    /**
     * Perform the Synthetic action.  There are no
     * Commands/MIME-bits or Responses available to
     * Synthetic Actions.
     *
     * @param actions the available set of actions.
     */
    public void handle(Session.SmtpResponseActions actions);
}
