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

package com.metavize.tran.virus;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.papi.imap.ImapTokenStream;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;


/**
 * Factory to create the protocol handler for IMAP
 * virus scanning
 */
public final class VirusImapFactory
  implements TokenHandlerFactory {

  
  private static final Logger m_logger =
    Logger.getLogger(VirusImapFactory.class);
      
  private WrappedMessageGenerator m_inWrapper =
    new WrappedMessageGenerator(VirusSettings.IN_MOD_SUB_TEMPLATE,
      VirusSettings.IN_MOD_BODY_TEMPLATE);

  private WrappedMessageGenerator m_outWrapper =
    new WrappedMessageGenerator(VirusSettings.OUT_MOD_SUB_TEMPLATE,
      VirusSettings.OUT_MOD_BODY_TEMPLATE);      

  private final VirusTransformImpl m_virusImpl;
  private final MailExport m_mailExport;    


  VirusImapFactory(VirusTransformImpl transform) {
    m_virusImpl = transform;
    /* XXX RBS I don't know if this will work */
    m_mailExport = MailExportFactory.factory().getExport( transform.getTid().getPolicy());    
  }


  public TokenHandler tokenHandler(TCPSession session) {
    
    boolean inbound = session.isInbound();  

    VirusIMAPConfig virusConfig = inbound?
      m_virusImpl.getVirusSettings().getIMAPInbound():
      m_virusImpl.getVirusSettings().getIMAPOutbound();

    if(!virusConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return new ImapTokenStream(session);
    }

    long timeout = inbound?m_mailExport.getExportSettings().getImapInboundTimeout():
      m_mailExport.getExportSettings().getImapOutboundTimeout();
    
    return new ImapTokenStream(session,
        new VirusImapHandler(
          timeout,
          timeout,
          m_virusImpl,
          virusConfig,
          inbound?m_inWrapper:m_outWrapper)
      );
  }
}
