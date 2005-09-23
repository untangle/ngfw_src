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

package com.metavize.tran.clamphish;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.papi.imap.ImapTokenStream;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.spam.SpamIMAPConfig;



public class PhishImapFactory implements TokenHandlerFactory
{


  private static final Logger m_logger =
    Logger.getLogger(PhishImapFactory.class);

  private final MailExport m_mailExport;
  private final ClamPhishTransform m_transform;

    PhishImapFactory(ClamPhishTransform transform) {
      m_transform = transform;
      /* XXX RBS I don't know if this will work */
      m_mailExport = MailExportFactory.factory().getExport( transform.getTid().getPolicy());    
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session) {
    
      boolean inbound = session.isInbound();
    
      SpamIMAPConfig config = inbound?
        m_transform.getSpamSettings().getIMAPInbound():
        m_transform.getSpamSettings().getIMAPOutbound();
    
      if(!config.getScan()) {
        m_logger.debug("Scanning disabled.  Return passthrough token handler");
        return new ImapTokenStream(session);
      }
    
      long timeout = inbound?m_mailExport.getExportSettings().getImapInboundTimeout():
        m_mailExport.getExportSettings().getImapOutboundTimeout();
    
      return new ImapTokenStream(session,
          new PhishImapHandler(
            session,
            timeout,
            timeout,
            m_transform,
            config)
        );
    }
}
