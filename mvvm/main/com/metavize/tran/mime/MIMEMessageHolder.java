 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id:$
  */
package com.metavize.tran.mime;
import java.io.IOException;


/**
 * Top-level holder class.  Maintains an
 * association between a MIMEMessage and its
 * MIMESource.  It is assumes that the
 * MIMESource contains the <b>entire</b>
 * MIMEMessage.
 */
public class MIMEMessageHolder {

  private MIMEMessage m_message;
  private MIMESource m_source;
  private boolean m_changed = false;
  

  public MIMEMessageHolder(MIMEMessage message,
    MIMESource source) {
    m_changed = message.isChanged();
    message.setObserver(new MIMEPartObserver() {
      public void mIMEPartChanged(MIMEPart part) {
        m_changed = true;
      }
    });
    m_source = source;
  }
    
  /**
   * Write-out the MIMEMessage.  If the 
   * message has not changed, the source is used.
   * Otherwise, the message's writeTo
   * method is invoked.
   *
   * @param out the output stream.
   */
  public void writeTo(MIMEOutputStream out)
    throws IOException {
    if(m_changed) {
      m_message.writeTo(out);
    }
    else {
      out.pipe(m_source.getInputStream());
    }
  }

  /**
   * Close this holder.  Also closes the 
   * source and MIMEMessage
   */
  public void close() {
    m_message.dispose();
    m_source.close();
    m_source = null;
    m_message = null;
  }

}