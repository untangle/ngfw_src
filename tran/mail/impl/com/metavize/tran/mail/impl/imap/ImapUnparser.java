/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.imap;

import com.metavize.tran.mail.impl.AbstractMailUnparser;
import com.metavize.mvvm.tapi.TCPSession;
import org.apache.log4j.Logger;

/**
 * Base class for the ImapClient/ServerUnparser
 */
abstract class ImapUnparser
  extends AbstractMailUnparser {

//  private final Logger m_logger = Logger.getLogger(ImapUnparser.class);

  protected ImapUnparser(TCPSession session,
    ImapCasing parent,
    boolean clientSide) {
    
    super(session, parent, clientSide, "imap");
  }

  /**
   * Accessor for the parent casing
   */
  protected ImapCasing getImapCasing() {
    return (ImapCasing) getParentCasing();
  }
}  