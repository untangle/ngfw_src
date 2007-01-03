/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.papi.smtp.sapi;

import com.untangle.tran.mail.papi.smtp.Response;

/**
 * Convienence implementation of ResponseCompletion
 * which simply passes back to the client.
 * 
 */
public class PassthruResponseCompletion
  implements ResponseCompletion {


  public void handleResponse(Response resp,
    Session.SmtpResponseActions actions) {
    actions.sendResponseToClient(resp);
  }

}