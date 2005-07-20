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
 * Class representing the "MAIL FROM:&lt;X>" Command.
 * <br>
 * <b>If we are to understand the ESMTP commands which modify
 * the MAIL line, this class must be modified.</b>
 */
public class MAILCommand 
  extends CommandWithEmailAddress {

  private static final String NULL_FROM_STR = "FROM:<>";

  /**
   * Construct a MAIL command from the given
   * arguments.
   *
   * @param cmdStr the string ("MAIL" or some
   *        with mixed case equivilant)
   * @param argStr.  The argument String.  Should
   *        be in the form "FROM:&lt;X>" where "X"
   *        must be a parsable address or blank.
   */
  public MAILCommand(String cmdStr,
    String argStr) throws ParseException {
    
    super(CommandType.MAIL, cmdStr, argStr);
    
    if(cmdStr == null) {
      //TODO bscott What should we do?  Fix this up?
      setCmdStr(NULL_FROM_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    cmdStr = cmdStr.trim();

    if(cmdStr.length() == 0) {
      //TODO bscott What should we do?  Fix this up?
      setCmdStr(NULL_FROM_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    else {
      //Strip-off the "from" if found
      //TODO bscott  This is a hack
      String cmdStrLower = cmdStr.toLowerCase();
      int toStrip = 0;
      if(cmdStrLower.startsWith("from:")) {
        cmdStr = cmdStr.substring(5);
      }
      else if(cmdStrLower.startsWith("from")) {
        cmdStr = cmdStr.substring(4);
      }
      EmailAddress addr = parseAddress(cmdStr);
      if(addr.isNullAddress()) {
        setCmdStr(NULL_FROM_STR);
        setAddress(addr);
      }
      else {
        setAddress(addr);
      }
    }
  }

  /**
   * Constructs a valid MAIL command with the
   * given address.  If the passed-in address
   * is null, then the output string will
   * become "MAIL FROM:<>".
   */
  public MAILCommand(EmailAddress addr)
    throws ParseException {
    super(CommandType.MAIL, "MAIL", null);
    if(addr == null || addr.isNullAddress()) {
      setAddress(EmailAddress.NULL_ADDRESS);
      setCmdStr(NULL_FROM_STR);
    }
    else {
      setAddress(addr);
      StringBuilder sb = new StringBuilder();
      sb.append("FROM:");
      sb.append(addr.toSMTPString());
      setCmdStr(sb.toString());
    }
  }
}