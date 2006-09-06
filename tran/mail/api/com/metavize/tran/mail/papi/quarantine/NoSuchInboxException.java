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

package com.metavize.tran.mail.papi.quarantine;
import java.io.Serializable;

/**
 * ...name says it all...
 */
public class NoSuchInboxException
  extends Exception
  implements Serializable {

  private final String m_accountName;

  public NoSuchInboxException(String accountName) {
    super("No such account \"" + accountName + "\"");
    m_accountName = accountName;
  }

  public String getAccountName() {
    return m_accountName;
  }
  
}