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

import static com.untangle.tran.util.Rfc822Util.*;
import static com.untangle.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.untangle.tran.token.ParseException;
import com.untangle.tran.token.Token;

/**
 * Because of classloader issues this class is public.  However,
 * it should really not be used other than in the casing.
 */
public class CommandParser {

  /**
   * Parse the buffer (which must have a complete line!)
   * into a Command.  May return a subclass
   * of Command for Commands with interesting arguments
   * we wish parsed.
   */
  public static Command parse(ByteBuffer buf)
    throws ParseException {

    //TODO bscott Shouldn't the command token always
    //     be 4 in length?  Should we make this some type
    //     of guard against evildooers?
    String cmdStr = consumeToken(buf);
    cmdStr=cmdStr==null?
      "":cmdStr.trim();
    eatSpace(buf);
    String argStr = consumeLine(buf);
    Command.CommandType type = Command.stringToCommandType(cmdStr);

    switch(type) {
      case MAIL:
        return new MAILCommand(cmdStr, argStr);
      case RCPT:
        return new RCPTCommand(cmdStr, argStr);
      case AUTH:
        return new AUTHCommand(cmdStr, argStr);
      default:
        return new Command(type, cmdStr, argStr);    
    }
  }

  public static void main(String[] args) throws Exception {
    String crlf = "\r\n";

    testParse("FOO moo" + crlf);
    testParse("\r" + crlf);
    testParse("FOO" + crlf);
    testParse("" + crlf);
    System.out.println(parse(ByteBuffer.wrap((" " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" \t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap(("FOO \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO x" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\t x" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \t x" + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO x " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO x  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO x\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO x \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO x\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO x \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO  x " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO  x \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO\tx " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO\tx \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO \tx " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx \t " + crlf).getBytes())).getCmdString());

    System.out.println(parse(ByteBuffer.wrap((" FOO \tx " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx  " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx\t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx\t " + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx \t" + crlf).getBytes())).getCmdString());
    System.out.println(parse(ByteBuffer.wrap((" FOO \tx \t " + crlf).getBytes())).getCmdString());

  }

  private static void testParse(String str) throws Exception {
    System.out.println("\n\n===================");
    System.out.println(str);
    Command c = parse(ByteBuffer.wrap(str.getBytes()));
    System.out.println("CMD: " + c.getCmdString());
    System.out.println("ARGS: " + c.getArgString());
  }
  
}