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

package com.metavize.tran.mail.papi.smtp;

import static com.metavize.tran.util.Rfc822Util.*;
import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.metavize.tran.mime.*;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;

/**
 * Class representing the "RCPT TO:&lt;X>" Command.
 * <br>
 * Understands a null address as a recipient, although
 * this is semantically nonsense as-per SMTP.
 * <br>
 * If we are to understand the ESMTP commands which modify
 * the RCPT line, this class must be modified.
 */
public class RCPTCommand
  extends CommandWithEmailAddress {

  private static final String NULL_TO_STR = "TO:<>";

  public RCPTCommand(String cmd,
    String argStr) throws ParseException {
    
    super(CommandType.RCPT, cmd, argStr);
    
    if(argStr == null) {
      //TODO bscott What should we do?  Fix this up?
      setArgStr(NULL_TO_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    argStr = argStr.trim();

    if(argStr.length() == 0) {
      //TODO bscott What should we do?  Fix this up?
      setArgStr(NULL_TO_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    else {
      //Strip-off the "to" if found
      //TODO bscott  This is a hack
      String argStrLower = argStr.toLowerCase();
      int toStrip = 0;
      if(argStrLower.startsWith("to:")) {
        argStr = argStr.substring(3);
      }
      else if(argStrLower.startsWith("to")) {
        argStr = argStr.substring(2);
      }
      EmailAddress addr = parseAddress(argStr);
      if(addr.isNullAddress()) {
        setArgStr(NULL_TO_STR);
        setAddress(addr);
      }
      else {
        setAddress(addr);
      }
    }
  }

  /**
   * Constructs a valid RCPT command with the
   * given address.  If the passed-in address
   * is null, then the output string will
   * become "RCPT TO:<>" (which is nonsense
   * but this class is not intended to enforce
   * protocol semantics, only command format).
   */
  public RCPTCommand(EmailAddress addr)
    throws ParseException {
    super(CommandType.MAIL, "RCPT", null);
    if(addr == null || addr.isNullAddress()) {
      setAddress(EmailAddress.NULL_ADDRESS);
      setArgStr(NULL_TO_STR);
    }
    else {
      setAddress(addr);
      StringBuilder sb = new StringBuilder();
      sb.append("TO:");
      sb.append(addr.toSMTPString());
      setArgStr(sb.toString());
    }
  }
}