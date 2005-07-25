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

import com.metavize.tran.mail.MessageBoundaryScanner;

import com.metavize.tran.mail.papi.smtp.*;
import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;
import com.metavize.tran.mime.*;


//TODO bscott find-out how we can get a callback to close the
//     open file
/**
 * <br>
 * Token Types:
 * <ul>
 * <li>PassThruToken</li>
 * <li>Chunk</li>
 * <li>Command
 *   <ul>
 *     <li>MAILCommand</li>
 *     <li>RCPTCommand</li>
 *     <li></li>
 *   </ul>
 * </li>
 * <li>BeginMIMEToken</li>
 * <li>ContinuedMIMEToken</li>
 * <li>WholeMIMEToken</li>
 * <li>Response</li>
 * <li></li>    
 * </ul>
 */
public class SmtpClientParser
  extends AbstractParser {

//  private final byte[] CRLF_DOT_CRLF = new byte[] {
//    (byte) CR, (byte) LF, (byte) DOT, (byte) CR, (byte) LF
//  };
//  private final byte[] CRLF_DOT_DOT_CRLF = new byte[] {
//    (byte) CR, (byte) LF, (byte) DOT, (byte) DOT, (byte) CR, (byte) LF
//  };  
//  private final byte[] CRLF_CRLF = new byte[] {
//    (byte) CR, (byte) LF, (byte) CR, (byte) LF
//  };  
    
  
  private final Pipeline pipeline;//For making temp files
  private final Logger m_logger = Logger.getLogger(SmtpClientParser.class);
  
  private enum SmtpClientState {
    COMMAND,
    HEADERS,
    BODY,
    PASSTHRU
  };
  private final SmtpCasing m_parentCasing;
  
  private SmtpClientState m_state = SmtpClientState.COMMAND;
  
  //Transient
  private MailTransaction m_tx;

  public SmtpClientParser(TCPSession session,
    SmtpCasing parent) {
    super(session, true);
    m_logger.debug("Created");
    m_parentCasing = parent;
    lineBuffering(false);
    pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());
  }
  

  
  public ParseResult parse(ByteBuffer buf) {
    m_parentCasing.traceParse(buf);
    
//    if(System.currentTimeMillis() > 0) {
//      m_logger.debug("**DEBUG*** Parse");
//      return new ParseResult(new Chunk(buf));
//    }
    ByteBuffer dup = buf.duplicate();
    try {
      m_logger.debug("BEGIN Call parseImpl");
      ParseResult ret = parseImpl(buf);
      m_logger.debug("ENDOF Call parseImpl");
      return ret;
    }
    catch(Exception ex) {
      //TODO bscott Clean-up transaction
      //TODO bscott Possibly (if we're still parsing headers)
      //     send along the magical token indicating that
      //     data is trapped in a file.
      m_logger.error("Unexpected exception parsing from client", ex);
      m_state = SmtpClientState.PASSTHRU;
      List<Token> toks = new LinkedList<Token>();
      toks.add(PassThruToken.PASSTHRU);
      toks.add(new Chunk(dup));
      return new ParseResult(toks, null);      
    }
  }
  
  
  private ParseResult parseImpl(ByteBuffer buf) 
    throws ParseException {
    
    m_logger.debug("BEGIN parseImpl.  State: " + m_state);
    
    List<Token> toks = new LinkedList<Token>();
    
    boolean done = false;
    
    while(!done && buf.hasRemaining()) {
      m_logger.debug("In while loop (looking for done and remaining in buffer)");
      switch(m_state) {
        case COMMAND:
          //TODO bscott we need a guard here to prevent
          //     obscenely long lines, and to prevent us from
          //     kicking-back the buffer to the caller 
          //     in ever-increasing sizes.  Otherwise, 
          //     an easy attack is to simply stream
          //     bytes w/o CRLF        
          m_logger.debug("COMMAND state");
          if(findCrLf(buf) >= 0) {//BEGIN Complete Command 
            Command cmd = CommandParser.parse(buf);
            m_logger.debug("Adding Command (\"" + cmd.getType() + "\") to tokens");
            toks.add(cmd);

            switch(cmd.getType()) {
              case MAIL:
                createTXIfNotOpen();
                //Add from to the transaction
                break;
              case RCPT:
                createTXIfNotOpen();
                //Add from to the transaction
                break;
              case DATA:
                m_logger.debug("entering DATA state");
                createTXIfNotOpen();
                m_state = SmtpClientState.HEADERS;
                try {
                  //TODO bscott FixThis
                  m_tx.file = pipeline.mktemp();//new File("MIME" + System.currentTimeMillis() + ".txt");//
                  m_tx.fOut = new FileOutputStream(m_tx.file);
                  m_tx.scanner = new MessageBoundaryScanner();
                  m_logger.debug("created temp file \"" + m_tx.file.getAbsolutePath() + "\" for MIME message");
                }
                catch(IOException ex) {
                  m_logger.error("Exception creating a temp file for MIME message", ex);
                  toks.add(PassThruToken.PASSTHRU);
                  toks.add(new Chunk(buf));
                  m_logger.debug("Send along passthrough");
                  return new ParseResult(toks, null);
                }
                //Go back and start evaluating the header bytes.
                break;
            }
          }//ENDOF Complete Command
          else {//BEGIN Not complete Command
            //TODO bscott see note above.  This is vulnerable
            //     to attack
            m_logger.debug("does not end with CRLF");
            done = true;
          }//ENDOF Not complete Command
          break;
        case HEADERS:
          m_logger.debug("HEADERS state");
          ByteBuffer dup = buf.duplicate();
          FileChannel fc = m_tx.fOut.getChannel();
          try {
            if(m_tx.scanner.processHeaders(buf, 1024*4)) {//TODO bscott a real value here
              m_logger.debug("Found header end.  Now Parse");
              //Record the SourceRecord
              int headersLen = buf.position() - dup.position();
              if(m_tx.scanner.isHeadersBlank()) {
                m_tx.headers = new MIMEMessageHeaders();
              }
              else {
                dup.limit(buf.position());
                m_logger.debug("Write remaining " + dup.remaining() + " header bytes to file");
                while(dup.hasRemaining()) {
                  fc.write(dup);
                }
                //TODO bscott remove this debug.
                m_logger.debug("Message file is now: " + m_tx.file.length() + " bytes long");
                parseHeaders();
              }
              m_logger.debug("Adding Begin MIME to token");
              toks.add(new BeginMIMEToken(m_tx.headers, m_tx.source, headersLen));
              m_state = SmtpClientState.BODY;
              if(m_tx.scanner.isEmptyMessage()) {
                toks.add(new ContinuedMIMEToken(true));
                m_state = SmtpClientState.COMMAND;
              }
              break;
            }
            else {
              m_logger.debug("Need more header bytes");
              dup.limit(buf.position());
              m_logger.debug("Write " + dup.remaining() + " header bytes to file");              
              while(dup.hasRemaining()) {
                fc.write(dup);
              }
              //TODO bscott remove debug
              m_logger.debug("Message file is now: " + m_tx.file.length() + " bytes long");
              done = true;
            }
          }
          catch(Exception ex) {
            m_logger.error("Exception parsing Headers", ex);
            //TODO bscott This is where we use Aaron's new TokenStreamer stuff
            //TODO bscott clean all this shit up, like the streams.
            return null;
          }
          break;
        case BODY:
          //TODO bscott remove debug
          m_logger.debug("BODY state.  Message file: " + m_tx.file.length() + " bytes long");
          m_logger.debug("BODY state " + buf.remaining() + " remaining");
          ByteBuffer bodyBuf = ByteBuffer.allocate(buf.remaining());
          boolean bodyEnd = m_tx.scanner.processBody(buf, bodyBuf);
          bodyBuf.flip();
          if(bodyEnd) {
            m_logger.debug("Found end of body");
            m_state = SmtpClientState.COMMAND;
          }
          else {
            done = true;
          }
          m_logger.debug("Adding continued MIME token with length: " + bodyBuf.remaining());
          toks.add(new ContinuedMIMEToken(bodyBuf, bodyEnd, false));
          break;
        case PASSTHRU:
          toks.add(PassThruToken.PASSTHRU);
          toks.add(new Chunk(buf));
          m_logger.debug("In passthru Returning simple chunk");
          return new ParseResult(toks, null);         
      }
    }

    //Handle any remainder
    if(buf.hasRemaining()) {
      buf.compact();
      if(buf.position() > 0 && buf.remaining() < 1024/*TODO bscott a real value*/) {
        ByteBuffer b = ByteBuffer.allocate(buf.capacity() + 1024);
        buf.flip();
        b.put(buf);
        buf = b;
      }
    }
    else {
      buf = null;
    }
    m_logger.debug("returning ParseResult with " +
      toks.size() + " tokens and a " + (buf==null?"null":"") + " buffer");
    return new ParseResult(toks, buf);
  }
                  
  public ParseResult parseEnd(ByteBuffer chunk)
    throws ParseException {
    if(chunk.hasRemaining()) {
      m_logger.warn("data trapped in read buffer: "
        + AsciiCharBuffer.wrap(chunk));
    }
    return new ParseResult();    
  }
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    m_parentCasing.endSession(true);
    return null;
  }   
  

  private void parseHeaders()
    throws Exception {
    
    
    FileMIMESource source = null;
    MIMEParsingInputStream in = null;
    try {
      source = new FileMIMESource(m_tx.file);
      in = source.getInputStream();
      m_tx.headers = MIMEMessageHeaders.parseMMHeaders(in, source);
      in.close();
      m_tx.source = source;
    }
    catch(IOException ex) {
      try {in.close();}catch(Exception ignore){}
      source.close();
      IOException ex2 = new IOException();
      ex.initCause(ex2);
      throw ex2;
    }

  }
  
  
  private void createTXIfNotOpen() {
    if(m_tx == null) {
      m_logger.debug("Creating new Transaction");
      m_tx = new MailTransaction();
    }
  }
  
  
  class MailTransaction {
    File file;
    FileOutputStream fOut;
    boolean isBlank = false;
    MIMEMessageHeaders headers;
    FileMIMESource source;
    MessageBoundaryScanner scanner;
    
    void flush() {
      try {fOut.flush();}catch(Exception ignore){}
    }
  }
  
}