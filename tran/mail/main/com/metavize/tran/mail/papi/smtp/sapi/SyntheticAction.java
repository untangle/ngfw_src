/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.smtp.sapi;


/**
 * A SyntheticAction is used when a Synthetic Response (@see SessionHandler)
 * must be issued.  The SyntheticAction is {@link #handle invoked}
 * by the session when it is appropriate (based on outstanding request
 * ordering) to issue a Response to the client.
 */
public interface SyntheticAction {


  /**
   * Perform the Synthetic action.  There are no
   * Commands/MIME-bits or Responses available to
   * Synthetic Actions.
   *
   * @param actions the available set of actions.
   */
  public void handle(Session.SmtpActions actions);

}