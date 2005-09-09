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

import org.apache.log4j.Logger;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.MetadataToken;
import com.metavize.tran.mail.papi.MIMEAccumulator;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import java.nio.ByteBuffer;


/**
 * Token reprsenting the Begining
 * of a MIME message.
 * <br>
 * Note that since this is a Metadata
 * Token, you must use {@link #toTCPStreamer toTCPStreamer}
 * to write this out
 * 
 */
public class BeginMIMEToken
  extends MetadataToken {

  private final Logger m_logger =
    Logger.getLogger(BeginMIMEToken.class);

  private MIMEAccumulator m_accumulator;
  private MessageInfo m_messageInfo;


  public BeginMIMEToken(MIMEAccumulator accumulator,
    MessageInfo messageInfo) {
    m_accumulator = accumulator;
    m_messageInfo = messageInfo;
    //TODO bscott ***DEBUG*** remove debug
    m_logger.debug("Created");
  }


  public MessageInfo getMessageInfo() {
    return m_messageInfo;
  }

  public MIMEAccumulator getMIMEAccumulator() {
    return m_accumulator;
  }



  /**
   * Get a TokenStreamer for the initial
   * contents of this message
   *
   * @param byteStuffer the byte stuffer used for initial bytes.  The
   *        stuffer will retain its state, so subsequent writes will
   *        cary-over any retained bytes.
   * 
   * @return the TCPStreamer
   */
  public TCPStreamer toTCPStreamer(ByteBufferByteStuffer byteStuffer) {
    return new ByteBtuffingTCPStreamer(m_accumulator.toTCPStreamer(),
      byteStuffer);
  }  

  //----------------- Inner Class -----------------------

  private class ByteBtuffingTCPStreamer
    implements TCPStreamer {
    
    private final TCPStreamer m_wrappedStreamer;
    private final ByteBufferByteStuffer m_bbbs;

    ByteBtuffingTCPStreamer(TCPStreamer wrapped,
      ByteBufferByteStuffer bbbs) {
      m_wrappedStreamer = wrapped;
      m_bbbs = bbbs;
    }
    
    public boolean closeWhenDone() {
      return m_wrappedStreamer.closeWhenDone();
    }
    
    public ByteBuffer nextChunk() {
      ByteBuffer next = m_wrappedStreamer.nextChunk();
      if(next != null) {
        ByteBuffer ret = ByteBuffer.allocate(next.remaining() +
          (m_bbbs.getLeftoverCount()*2));
        m_bbbs.transfer(next, ret);
        return ret;
      }
      return next;
    }    
  }
}