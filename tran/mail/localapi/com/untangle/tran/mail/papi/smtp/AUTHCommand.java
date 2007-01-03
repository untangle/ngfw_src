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

package com.untangle.tran.mail.papi.smtp;

import com.untangle.tran.token.ParseException;


/**
 * Class reprsenting an SMTP "AUTH" Command (RFC 2554)
 */
public class AUTHCommand
  extends Command {

  private String m_mechanismName;
  private String m_initialResponse;

  public AUTHCommand(String cmdStr,
    String argStr) throws ParseException {
    super(CommandType.AUTH, cmdStr, argStr);
    parseArgStr();
  }

  /**
   * Get the name of the SASL mechanism.
   */
  public String getMechanismName() {
    return m_mechanismName;
  }

  /**
   * Note that the initial "response" (dumb name, but from the spec)
   * is still base64 encoded.
   */
  public String getInitialResponse() {
    return m_initialResponse;
  }

  @Override
  protected void setArgStr(String argStr) {
    super.setArgStr(argStr);
    parseArgStr();
  }

  private void parseArgStr() {
    String argStr = getArgString();
    if(argStr == null) {
      return;
    }
    argStr = argStr.trim();
    int spaceIndex = argStr.indexOf(' ');
    if(spaceIndex == -1) {
      m_mechanismName = argStr;
    }
    else {
      m_mechanismName = argStr.substring(0, spaceIndex);
      m_initialResponse = argStr.substring(spaceIndex+1, argStr.length());
    }
  }
}