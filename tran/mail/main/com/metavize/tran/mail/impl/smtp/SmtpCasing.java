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

package com.metavize.tran.mail.impl.smtp;

import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import java.nio.*;
import java.io.*;
import org.apache.log4j.Logger;
import java.nio.channels.*;


public class SmtpCasing extends AbstractCasing {

  private final Logger m_logger = Logger.getLogger(SmtpCasing.class);

  private final Parser m_parser;
  private final Unparser m_unparser;

  private static final boolean TRACE = true;

  private final boolean m_trace;
  private FileOutputStream m_fOut;
  private FileChannel m_channel;
  private int m_closeCount = 0;

  // constructors -----------------------------------------------------------

  public SmtpCasing(TCPSession session, boolean clientSide) {
    if(clientSide) {
      TransactionTracker tracker = new TransactionTracker();
      m_parser = new SmtpClientParser(session, this, tracker);
      m_unparser = new SmtpClientUnparser(session, this, tracker);
    }
    else {
      m_parser = new SmtpServerParser(session, this);
      m_unparser = new SmtpServerUnparser(session, this);    
    }

    m_trace = TRACE;//Someday, perhaps a hidden property
      
    if(m_trace) {
      try {
        String fileName = session.id() + "_" + (clientSide?"client":"server") +
          "_facing.trace";
        File file = new File(getTraceRoot(), fileName);
        m_fOut = new FileOutputStream(file);
        m_channel = m_fOut.getChannel();
      }
      catch(Exception ex) {
        m_logger.error("Cannot create trace file", ex);
        m_channel = null;
      }
    }
  }

  private static File s_traceRoot;
  private static ByteBuffer s_parsePrefix = ByteBuffer.wrap((
    System.getProperty("line.separator") +
    "============= PARSER =============" +
    System.getProperty("line.separator")).getBytes());
  private static ByteBuffer s_unParsePrefix = ByteBuffer.wrap((
    System.getProperty("line.separator") +
    "============ UNPARSER ============" +
    System.getProperty("line.separator")).getBytes());
  private static ByteBuffer s_end = ByteBuffer.wrap((
    System.getProperty("line.separator") +
    "============== END ===============" +
    System.getProperty("line.separator")).getBytes());       
  
  private synchronized static File getTraceRoot() {
    if(s_traceRoot == null) {
      s_traceRoot = new File(System.getProperty("user.dir"), "smtpTrace");
      if(!s_traceRoot.exists()) {
        s_traceRoot.mkdirs();
      }
    }
    return s_traceRoot;
  }

  // Casing methods ---------------------------------------------------------

  public Parser parser() {
    return m_parser;
  }

  public Unparser unparser() {
    return m_unparser;
  }

  /**
   * Only public for classloader issues
   */
  public void traceParse(ByteBuffer buf) {
    if(m_trace) {
      traceWrite(s_parsePrefix, buf);
    }
  }
  /**
   * Only public for classloader issues
   */
  public void traceUnparse(ByteBuffer buf) {
    if(m_trace) {
      traceWrite(s_unParsePrefix, buf);
    }
  }

  public void endSession(boolean calledFromParser) {
    if(m_trace) {
      if(++m_closeCount > 1) {
        closeTrace();
      }
    }
  }

  private synchronized void closeTrace() {
    traceWrite(s_end, null);
    if(m_channel != null) {
      try {m_fOut.flush();}catch(Exception ignore){}
      try {m_channel.close();}catch(Exception ignore){}
      try {m_fOut.close();}catch(Exception ignore){}
      m_channel = null;
    }
  }

  private synchronized void traceWrite(ByteBuffer preamble, ByteBuffer data) {
    if(m_channel == null) {
      return;
    }
    preamble.rewind();
    ByteBuffer buf = data==null?null:data.duplicate();
    try {
      while(preamble.hasRemaining()) {
        m_channel.write(preamble);
      }
      if(data != null) {
        while(buf.hasRemaining()) {
          m_channel.write(buf);
        }
      }
    }
    catch(Exception ex) {
      m_logger.error("Error writing to trace file", ex);
      m_channel = null;
    }
  }
}