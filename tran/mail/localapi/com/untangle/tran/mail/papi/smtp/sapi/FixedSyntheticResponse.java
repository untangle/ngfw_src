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

import java.nio.ByteBuffer;
import com.untangle.tran.mail.papi.smtp.Response;


/**
 * A fixed response (e.g. "250 OK") which will be sent.  For simplicity,
 * this is a single-line response.
 * <br><br>
 * For details on what the heck this is good for, check out the docs on
 * {@link com.untangle.tran.mail.papi.smtp.sapi.SessionHandler SessionHandler}
 */
public class FixedSyntheticResponse
  implements SyntheticResponse {

  private Response m_resp;

  /**
   * Copnstruct a new (single-line) response with the given
   * code (e.g. "250") and the given text (e.g. "OK").
   */
  public FixedSyntheticResponse(int code, String cmdString) {
    m_resp = new Response(code, cmdString);
  }

  
  public void handle(Session.SmtpResponseActions actions) {
    actions.sendResponseToClient(m_resp);
  }

}