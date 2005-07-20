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
 * Base class of a Command which holds a parsed
 * EmailAddress (parsed from the arguments).
 */
public abstract class CommandWithEmailAddress
  extends Command {

  private EmailAddress m_address;
  
  protected CommandWithEmailAddress(CommandType type,
    String cmdStr,
    String argStr) throws ParseException {
    super(type, cmdStr, argStr);
  }

  protected void setAddress(EmailAddress address) {
    m_address = address;
  }

  /**
   * Get the EmailAddress parsed from this Command.
   */
  public EmailAddress getAddress() {
    return m_address;
  }

  /**
   * Helper method to parse an address read from
   * a Command argument.  Subclasses using this method
   * must tokenize away any leading stuff ("FROM:", "TO:")
   * as well as any ESMTP trailing tokens.  In other words,
   * this should be one and only one address.
   * <br>
   * Leading spaces are trimmed.
   * <br>
   * Will never return null.
   */
  protected static EmailAddress parseAddress(String str)
    throws ParseException {
    if(str == null) {
      return EmailAddress.NULL_ADDRESS;
    }
    str = str.trim();
    if(0 == str.indexOf('<')) {
      str = str.substring(1);
    }
    if(str.length()-1 == str.indexOf('>')) {
      str = str.substring(0, str.length()-1);
    }
    if("".equals(str.trim())) {
      return EmailAddress.NULL_ADDRESS;
    }
    try {
      return EmailAddress.parse(str);
    }
    catch(BadEmailAddressFormatException ex) {
      throw new ParseException(ex);
    }
  }
}