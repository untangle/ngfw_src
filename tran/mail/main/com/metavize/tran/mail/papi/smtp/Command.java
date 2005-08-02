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

//import static com.metavize.tran.util.Rfc822Util.*;
import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;


/**
 * Class reprsenting an SMTP Command issued
 * by a client.
 */
public class Command
  implements Token {

  /**
  * Enumeration of the SMTP Commands we know about (not
  * that we accept all of them).
  */
  public enum CommandType {
    HELO,
    EHLO,
    MAIL,
    RCPT,
    DATA,
    RSET,
    QUIT,
    SEND,//
    SOML,//
    SAML,//
    TURN,//
    VRFY,
    EXPN,
    HELP,
    NOOP,
    SIZE,
    STARTTLS,
    UNKNOWN
  };

  //==========================================
  // Warning - if you add to the list above,
  // you must also modify the
  // "stringToCommandType" method
  //==========================================  

  private final CommandType m_type;
  private final String m_cmdStr;
  private String m_argStr;

  public Command(CommandType type,
    String cmdStr,
    String argStr) throws ParseException {
    m_type = type;
    m_cmdStr = cmdStr;
    m_argStr = argStr;
  }

  public Command(CommandType type) {
    m_type = type;
    m_cmdStr = type.toString();
    m_argStr = null;
  }

  /**
   * Get the string of the command (i.e. "HELO", "RCPT").
   */
  public String getCmdString() {
    return m_cmdStr;
  }

  protected void setArgStr(String argStr) {
    m_argStr = argStr;
  }


  /**
   * Get the argument to a command.  For example,
   * in "MAIL FROM:<>" "FROM:<>" is the argument. This may
   * be null.
   */
  public String getArgString() {
    return m_argStr;
  }

  /**
   * Get the type of the command.  Be warned -
   * the type may be "UNKNOWN"
   */
  public CommandType getType() {
    return m_type;
  }

  /**
   * Convert the command back to a valid line (with
   * terminator).
   * This is done by appending the type with
   * the results of {@link #getArgString getArgString()}.
   */
  public ByteBuffer getBytes() {
    //Do a bit of fixup on the string
    String cmdStr = m_type.toString();
    if(getType() == CommandType.UNKNOWN) {
      cmdStr = m_cmdStr;
    }

    String argStr = getArgString();
    
    ByteBuffer buf = ByteBuffer.allocate(
      cmdStr.length()/*always 4?*/ +
      (argStr == null?(0):(argStr.length() + 1)) +
      3);

    buf.put(cmdStr.getBytes());
    
    if(argStr != null) {
      buf.put((byte)SP);
      buf.put(argStr.getBytes());
    }
    buf.put((byte)CR);
    buf.put((byte)LF);

    buf.flip();

    return buf;
  }

  /**
   * Converts the given String to a CommandType.  Note that
   * the type may come back as "UNKNOWN".
   */
  public static CommandType stringToCommandType(String cmdStr) {

    //Commands, aligned with their enum type.
  
    if(cmdStr.equalsIgnoreCase("HELO")) {
      return CommandType.HELO;
    }
    if(cmdStr.equalsIgnoreCase("EHLO")) {
      return CommandType.EHLO;
    }
    if(cmdStr.equalsIgnoreCase("MAIL")) {
      return CommandType.MAIL;
    }
    if(cmdStr.equalsIgnoreCase("RCPT")) {
      return CommandType.RCPT;
    }
    if(cmdStr.equalsIgnoreCase("DATA")) {
      return CommandType.DATA;
    }
    if(cmdStr.equalsIgnoreCase("RSET")) {
      return CommandType.RSET;
    }
    if(cmdStr.equalsIgnoreCase("QUIT")) {
      return CommandType.QUIT;
    }
    if(cmdStr.equalsIgnoreCase("SEND")) {
      return CommandType.SEND;
    }
    if(cmdStr.equalsIgnoreCase("SOML")) {
      return CommandType.SOML;
    }
    if(cmdStr.equalsIgnoreCase("SAML")) {
      return CommandType.SAML;
    }
    if(cmdStr.equalsIgnoreCase("TURN")) {
      return CommandType.TURN;
    }
    if(cmdStr.equalsIgnoreCase("VRFY")) {
      return CommandType.VRFY;
    }
    if(cmdStr.equalsIgnoreCase("EXPN")) {
      return CommandType.EXPN;
    }
    if(cmdStr.equalsIgnoreCase("HELP")) {
      return CommandType.HELP;
    }
    if(cmdStr.equalsIgnoreCase("NOOP")) {
      return CommandType.NOOP;
    }
    if(cmdStr.equalsIgnoreCase("SIZE")) {
      return CommandType.SIZE;
    }
    if(cmdStr.equalsIgnoreCase("STARTTLS")) {
      return CommandType.STARTTLS;
    }     
    return CommandType.UNKNOWN;                                                       
  }
}