/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.smtp;

import com.metavize.tran.mail.impl.AbstractMailUnparser;
import org.apache.log4j.Logger;
import com.metavize.mvvm.tapi.TCPSession;

/**
 * Base class for the SmtpClient/ServerUnparser
 */
abstract class SmtpUnparser
  extends AbstractMailUnparser {

  private final Logger m_logger = Logger.getLogger(SmtpUnparser.class);
  private CasingSessionTracker m_tracker;

  protected SmtpUnparser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker,
    boolean clientSide) {
    
    super(session, parent, clientSide, "smtp");
    m_tracker = tracker;    
  }

  SmtpCasing getSmtpCasing() {
    return (SmtpCasing) getParentCasing();
  }

  CasingSessionTracker getSessionTracker() {
    return m_tracker;
  }  
}