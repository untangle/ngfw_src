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
  public MAILCommand(String cmd,
    String argStr) throws ParseException {
    
    super(CommandType.MAIL, cmd, argStr);
    
    if(argStr == null) {
      //TODO bscott What should we do?  Fix this up?
      setArgStr(NULL_FROM_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    argStr = argStr.trim();

    if(argStr.length() == 0) {
      //TODO bscott What should we do?  Fix this up?
      setArgStr(NULL_FROM_STR);
      setAddress(EmailAddress.NULL_ADDRESS);
    }
    else {
      //Strip-off the "from" if found
      //TODO bscott  This is a hack
      String argStrLower = argStr.toLowerCase();
      if(argStrLower.startsWith("from:")) {
        argStr = argStr.substring(5);
      }
      else if(argStrLower.startsWith("from")) {
        argStr = argStr.substring(4);
      }
      EmailAddress addr = parseAddress(argStr);
      if(addr.isNullAddress()) {
        setArgStr(NULL_FROM_STR);
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
      setArgStr(NULL_FROM_STR);
    }
    else {
      setAddress(addr);
      StringBuilder sb = new StringBuilder();
      sb.append("FROM:");
      sb.append(addr.toSMTPString());
      setArgStr(sb.toString());
    }
  }

  protected void setAddress(EmailAddress address) {
    super.setAddress(address);
    setArgStr("FROM:" + address.toSMTPString());
  }

  


/*

//TESTING CODE  
  
  public static void main(String[] args)
    throws Exception {
    String[] tests = new String[] {
      "FROM:foo@moo.com",
      "FROM:<foo@moo.com",
      "FROM:foo@moo.com>",
      "FROM:<foo@moo.com>",
      "FROM:<>",
      "FROM: foo@moo.com",
      "FROM: <foo@moo.com",
      "FROM: foo@moo.com>",
      "FROM: <foo@moo.com>",
      "FROM: <>",
      "from foo@moo.com",
      "from <foo@moo.com",
      "from foo@moo.com>",
      "from <foo@moo.com>",
      "from <>",
      "fromfoo@moo.com",
      "from<foo@moo.com",
      "fromfoo@moo.com>",
      "from<foo@moo.com>",
      "from<>"
    };
    for(String s : tests) {
      System.out.println("Address: " +s);
      System.out.println("   Became \"" + new MAILCommand("MAIL", s).getAddress() + "\"");
    }
  }
*/  
}