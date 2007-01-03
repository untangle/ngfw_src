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

package com.untangle.tran.spam;

import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailExportFactory;
import com.untangle.tran.mail.papi.imap.ImapTokenStream;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class SpamImapFactory
  implements TokenHandlerFactory {

  private final Logger m_logger = Logger.getLogger(getClass());

  private final SpamImpl m_impl;
  private final MailExport m_mailExport;
    private SafelistTransformView m_safelist;

  SpamImapFactory(SpamImpl impl) {
    m_impl = impl;
    /* XXX RBS I don't know if this will work */
    m_mailExport = MailExportFactory.factory().getExport();
    m_safelist = m_mailExport.getSafelistTransformView();
  }


  public TokenHandler tokenHandler(TCPSession session) {

    boolean inbound = session.isInbound();

    SpamIMAPConfig config = (!inbound)?
      m_impl.getSpamSettings().getIMAPInbound():
      m_impl.getSpamSettings().getIMAPOutbound();

    if(!config.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return new ImapTokenStream(session);
    }

    long timeout = (!inbound)?m_mailExport.getExportSettings().getImapInboundTimeout():
      m_mailExport.getExportSettings().getImapOutboundTimeout();

    return new ImapTokenStream(session,
        new SpamImapHandler(
          session,
          timeout,
          timeout,
          m_impl,
          config,
          m_safelist));
  }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
