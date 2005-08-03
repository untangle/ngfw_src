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

import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;

import com.metavize.tran.mail.papi.smtp.*;
import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.ASCIIUtil.*;
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


/**
 * ...name says it all...
 */
class SmtpClientParser
  extends SmtpParser {

  private final Logger m_logger = Logger.getLogger(SmtpClientParser.class);
  
  private enum SmtpClientState {
    COMMAND,
    HEADERS,
    BODY
  };
  
  //Transient
  private SmtpClientState m_state = SmtpClientState.COMMAND;
  private MIMEAccumulator m_mimeAccumulator;


  SmtpClientParser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker) {
    
    super(session, parent, tracker, true);
    
    m_logger.debug("Created");
    lineBuffering(false);
  }
  

  
  public ParseResult parse(ByteBuffer buf) {

    //===============================================
    // This method is very procedural, to make
    // cleanup after errors easier.  In general,
    // there are a lot of helper functions called
    // which return true/false.  Most of these operate
    // on the MIMEAccumulator data member.  If false
    // is returned from these methods, this method
    // performs cleanup and enters passthru mode.
    //   -wrs 7/05
    //
      
//    m_logger.debug("parse.  State: " + m_state);

    //Do tracing stuff
    getSmtpCasing().traceParse(buf);

    //Check for passthru
    if(isPassthru()) {
      m_logger.debug("Passthru buffer (" + buf.remaining() + " bytes)");
      return new ParseResult(new Chunk(buf));
    }       
    
    List<Token> toks = new LinkedList<Token>();
    boolean done = false;
    
    while(!done && buf.hasRemaining()) {
      m_logger.debug("Draining tokens from buffer (" + toks.size() +
        " tokens so far)");

      if(isPassthru()) {
        return new ParseResult(new Chunk(buf));
      }         
        
      switch(m_state) {
      
        //==================================================
        case COMMAND:
          //TODO bscott we need a guard here to prevent
          //     obscenely long lines, and to prevent us from
          //     kicking-back the buffer to the caller 
          //     in ever-increasing sizes.  Otherwise, 
          //     an easy attack is to simply stream
          //     bytes w/o CRLF        
//          m_logger.debug(m_state + " state");
          if(findCrLf(buf) >= 0) {//BEGIN Complete Command
          
            //Parse the next command.  If there is a parse error,
            //pass along the original chunk
            ByteBuffer dup = buf.duplicate();
            Command cmd = null;
            try {
              cmd = CommandParser.parse(buf);
            }
            catch(ParseException pe) {
              //Duplicate the bad buffer
              dup.limit(findCrLf(dup) + 2);
              ByteBuffer badBuf = ByteBuffer.allocate(dup.remaining());
              badBuf.put(dup);
              badBuf.flip();
              //Position the "real" buffer beyond the bad point.
              buf.position(dup.position());

              m_logger.error("Exception parsing command line \"" +
                ASCIIUtil.bbToString(badBuf) + "\".  Pass to server and monitor response", pe);

              cmd = new UnparsableCommand(badBuf);
                
              getSessionTracker().commandReceived(cmd,
                new CommandParseErrorResponseCallback(badBuf));

              toks.add(cmd);
              break;
            }

            //If we're here, we have a legitimate command
            toks.add(cmd);
            m_logger.debug("Received command: " + cmd.toDebugString());
            
            if(cmd.getType() == Command.CommandType.STARTTLS) {
              m_logger.debug("Enqueue observer for response to STARTTLS, " +
                "to go into passthru if accepted");
              getSessionTracker().commandReceived(cmd, new TLSResponseCallback());
            }
            else if(cmd.getType() == Command.CommandType.DATA) {
              m_logger.debug("entering data transmission (DATA)");
              if(!openMIMEAccumulator()) {
                //Error opening the temp file.  The
                //error has been reported and the temp file
                //cleaned-up
                m_logger.debug("Declare passthru as we cannot buffer MIME");
                declarePassthru();
                toks.add(PassThruToken.PASSTHRU);
                toks.add(new Chunk(buf));
                return new ParseResult(toks, null);                
              }
              m_logger.debug("Change state to " +
                SmtpClientState.HEADERS + ".  Enqueue response handler in case DATA " +
                "command rejected (returning us to " + SmtpClientState.COMMAND + ")");
              getSessionTracker().commandReceived(cmd, new DATAResponseCallback());                
              changeState(SmtpClientState.HEADERS);
              //Go back and start evaluating the header bytes.
            }
            else {
              getSessionTracker().commandReceived(cmd);
            }            
          }//ENDOF Complete Command
          else {//BEGIN Not complete Command
            //TODO bscott see note above.  This is vulnerable
            //     to attack
            m_logger.debug("Command line does not end with CRLF.  Need more bytes");
            done = true;
          }//ENDOF Not complete Command
          break;
          
        //==================================================
        case HEADERS:
//          m_logger.debug(m_state + " state. buf remaining " + buf.remaining());
          getSessionTracker().beginMsgTransmission();
          
          //Duplicate the buffer, in case we have a problem
          ByteBuffer dup = buf.duplicate();
          boolean endOfHeaders = false;
          try {
            endOfHeaders = m_mimeAccumulator.getScanner().processHeaders(buf, 1024*4);//TODO bscott a real value here
          }
          catch(LineTooLongException ltle) {
            m_logger.error("Exception looking for headers end", ltle);
            m_mimeAccumulator.closeFromError();
            m_mimeAccumulator = null;
            declarePassthru();
            toks.add(PassThruToken.PASSTHRU);
            toks.add(new Chunk(dup));
            return new ParseResult(toks, null);
          }

          //If we're here, we didn't get a line which was too long.  Write
          //what we have to disk.
          ByteBuffer dup2 = dup.duplicate();
          dup2.limit(buf.position());

          if(m_mimeAccumulator.getScanner().isHeadersBlank()) {
            //TODO bscott remove debugging (yet again)
            int len = dup.remaining()>80?80:dup.remaining();
            ByteBuffer dup3 = dup.duplicate();
            dup3.limit(dup3.position() + len);
            m_logger.debug("Headers are blank (first " +
              len + " bytes \"" + ASCIIUtil.bbToString(dup3) + "\")");
          }
          else {
            m_logger.debug("About to write the " +
              (endOfHeaders?"last":"next") + " " +
              dup2.remaining() + " header bytes to disk");
          }

          if(!writeHeaderBytesToFile(dup2)) {
            m_logger.error("Unable to write header bytes to disk.  Enter passthru");
            //Get any bytes trapped in the file
            ByteBuffer trapped = drainFileToByteBuffer();
            //Nuke the accumulator
            m_mimeAccumulator.closeFromError();
            //Passthru
            declarePassthru();
            toks.add(PassThruToken.PASSTHRU);
            if(trapped != null && trapped.remaining() > 0) {
              toks.add(new Chunk(trapped));
            }
            toks.add(new Chunk(dup));
            return new ParseResult(toks, null);            
          }

          if(endOfHeaders) {//BEGIN End of Headers
            if(!parseHeaders()) {//BEGIN Header PArse Error
              m_logger.error("Unable to parse headers.  Enter passthru");
              //Get any bytes trapped in the file
              ByteBuffer trapped = drainFileToByteBuffer();
              //Nuke the accumulator
              m_mimeAccumulator.closeFromError();
              //Passthru
              declarePassthru();
              toks.add(PassThruToken.PASSTHRU);
              if(trapped != null && trapped.remaining() > 0) {
                toks.add(new Chunk(trapped));
              }
              toks.add(new Chunk(dup));
              return new ParseResult(toks, null);                
            }//ENDOF Header PArse Error
            int headersLength = (int) m_mimeAccumulator.getFile().length();
            m_logger.debug("Parsed headers successfully (length " + headersLength + ")");
            toks.add(new BeginMIMEToken(m_mimeAccumulator.getHeaders(),
              m_mimeAccumulator.getFileSource(),
              headersLength,
              createMessageInfo()));
            changeState(SmtpClientState.BODY);
            if(m_mimeAccumulator.getScanner().isEmptyMessage()) {
              m_logger.debug("Message blank.  Skip to reading commands");
              toks.add(new ContinuedMIMEToken(true));
              changeState(SmtpClientState.COMMAND);
            }
            m_mimeAccumulator.closeNormal();            
          }//ENDOF End of Headers
          else {
            m_logger.debug("Need more header bytes");
            done = true;
          }
          break;

        //==================================================
        case BODY:
//          m_logger.debug(m_state + " state");
          ByteBuffer bodyBuf = ByteBuffer.allocate(buf.remaining());
          boolean bodyEnd = m_mimeAccumulator.getScanner().processBody(buf, bodyBuf);
          bodyBuf.flip();
          if(bodyEnd) {
            m_logger.debug("Found end of body");
            m_mimeAccumulator.closeNormal();
            m_mimeAccumulator = null;
            changeState(SmtpClientState.COMMAND);
          }
          else {
            done = true;
          }
          m_logger.debug("Adding continued MIME token with length: " + bodyBuf.remaining());
          toks.add(new ContinuedMIMEToken(bodyBuf, bodyEnd, false));
          break;
      }
    }

    //Compact the buffer
    buf = compactIfNotEmpty(buf);

    if(buf == null) {
      m_logger.debug("returning ParseResult with " +
        toks.size() + " tokens and a null buffer");    
    }
    else {
      m_logger.debug("returning ParseResult with " +
        toks.size() + " tokens and a buffer with " +
        buf.remaining() + " remaining (" +
        buf.position() + " to be seen on next invocation)");
    }
    return new ParseResult(toks, buf);
  }

  @Override
  public TokenStreamer endSession() {
    if(m_mimeAccumulator != null) {
      //TODO bscott temp debugging
      
      File f = m_mimeAccumulator.getFile();
      if(f != null) {
        m_logger.debug("Unexpected closed in state " + m_state +
          " with " + f.length() + " bytes in MIME file");
      }
      else {
        m_logger.debug("Unexpected closed in state " + m_state);      
      }
/*      
      FileOutputStream fOut = null;
      FileInputStream fIn = null;
      try {
        m_logger.debug("MIME file is " + m_mimeAccumulator.getFile().length() + " bytes long");
        File f = File.createTempFile("SMTP_DEBUG", ".tmp");
        fOut = new FileOutputStream(f);
        fIn = new FileInputStream(m_mimeAccumulator.getFile());
        byte[] transferBuf = new byte[1024];
        int read = fIn.read(transferBuf);
        while(read > 0) {
          fOut.write(transferBuf, 0, read);
          read = fIn.read(transferBuf);
        }
        fOut.flush();
        fOut.close();
        fIn.close();
        m_logger.debug("Wrote MIME file for session to file " +
          f.getAbsolutePath());
      }
      catch(Exception ex) {
        try {fOut.close();}catch(Exception ignore){}
        try {fIn.close();}catch(Exception ignore){}
      }
*/      
      m_mimeAccumulator.closeFromError();
    }
    return super.endSession();
  }

  private void changeState(SmtpClientState newState) {
    m_logger.debug("Change state " +
      m_state + "->" + newState);
    m_state = newState;
  }


  /**
   * Callback if TLS starts
   */
  private void tlsStarting() {
    m_logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
    declarePassthru();//Inform the parser of this state
  }

  //================ Inner Class =================

  /**
   * Callback registered with the CasingSessionTracker
   * for the response to the DATA command
   */
  class DATAResponseCallback
    implements CasingSessionTracker.ResponseAction {
    public void response(int code) {
      if(code < 400) {
        m_logger.debug("DATA command accepted");
      }
      else {
        m_logger.debug("DATA command rejected");
        m_mimeAccumulator.closeFromError();
        m_mimeAccumulator = null;
        changeState(SmtpClientState.COMMAND);
      }
    }    
  }  

  //================ Inner Class =================

  /**
   * Callback registered with the CasingSessionTracker
   * for the response to the STARTTLS command
   */
  class TLSResponseCallback
    implements CasingSessionTracker.ResponseAction {
    public void response(int code) {
      if(code < 300) {
        tlsStarting();
      }
      else {
        m_logger.debug("STARTTLS command rejected.  Do not go into passthru");
      }
    }    
  }

  //================ Inner Class =================

  /**
   * Callback registered with the CasingSessionTracker
   * for the response to a command we could not parse.
   * If the response can be parsed, and it is an error,
   * we do not go into passthru.  If the response is positive,
   * then we go into passthru.
   */
  class CommandParseErrorResponseCallback
    implements CasingSessionTracker.ResponseAction {

    private String m_offendingCommand;

    CommandParseErrorResponseCallback(ByteBuffer bufWithOffendingLine) {
      m_offendingCommand = bbToString(bufWithOffendingLine);
    }
    
    public void response(int code) {
      if(code < 300) {
        m_logger.error("Could not parse command line \"" +
          m_offendingCommand + "\" yet accepted by server.  Parser error.  Enter passthru");
        declarePassthru();
      }
      else {
        m_logger.debug("Command \"" + m_offendingCommand + "\" unparsable, and rejected " +
          "by server.  Do not enter passthru (assume errant client)");
      }
    }    
  }   
  
  /**
   * Open the MIME accumulator.  If there was an error,
   * the MIMEAccumulator (m_mimeAccumulator) is not
   * set as a data member and any files/streams
   * are cleaned-up.
   * 
   * @return false if there was an error creating the file.
   */
  private boolean openMIMEAccumulator() {
    m_logger.debug("Opening temp file to buffer MIME");
    File file = null;
    FileOutputStream fOut = null;
    try {
      file = getPipeline().mktemp();
      fOut = new FileOutputStream(file);
      m_mimeAccumulator = new MIMEAccumulator(file, fOut);
      return true;
    }
    catch(IOException ex) {
      m_logger.error("Exception creating a temp file for MIME message", ex);
      try {fOut.close();}catch(Exception ignore){}
      try {file.delete();}catch(Exception ignore){}
      m_mimeAccumulator = null;
      return false;
    }    
  }
  

  /**
   * Helper method to break-out the
   * creation of a MessageInfo
   */
  private MessageInfo createMessageInfo() {

    MessageInfo ret = MessageInfo.fromMIMEMessage(m_mimeAccumulator.getHeaders(),
      getSession().id(),
      getSession().serverPort());
    //Add anyone from the transaction
    SmtpTransaction smtpTx = getSessionTracker().getCurrentTransaction();
    if(smtpTx == null) {
      m_logger.error("Transaction tracker returned null for current transaction");
    }
    else {
      //Transfer the FROM
      if(smtpTx.getFrom() != null && !smtpTx.getFrom().isNullAddress()) {
        ret.addAddress(AddressKind.ENVELOPE_FROM,
          smtpTx.getFrom().getAddress(),
          smtpTx.getFrom().getPersonal());
      }
      List<EmailAddress> txRcpts = smtpTx.getRecipients(false);
      for(EmailAddress addr : txRcpts) {
        if(addr.isNullAddress()) {
          continue;
        }
        ret.addAddress(AddressKind.ENVELOPE_TO, addr.getAddress(), addr.getPersonal());
      }
    }
    return ret;
  }

  /**
   * Returns true if the headers were parsed correctly.
   * As a side effect, the MIMEAccumulator is "completed"
   * by having its MIMESource set.
   *
   * If there is an error, the MIMESource is not set
   * and the headers are blank.  The MIMEAccumulator
   * is not cleaned-up, but there should be no temp
   * streams open.
   *
   *
   * @return false if there was an error   
   */
  private boolean parseHeaders() {
    FileMIMESource source = null;
    MIMEParsingInputStream in = null;
    try {
      source = new FileMIMESource(m_mimeAccumulator.getFile());
      in = source.getInputStream();
      m_mimeAccumulator.setHeaders(MIMEMessageHeaders.parseMMHeaders(in, source));
      in.close();
      m_mimeAccumulator.setFileSource(source);      
      return true;
    }
    catch(Exception ex) {
      m_logger.error("Error parsing MIMEHeaders", ex);
      try {in.close();}catch(Exception ignore){}
      try {source.close();}catch(Exception ignore){}
      return false;
    }
  }

  /**
   * Drains the buffer into the File used to accumulate
   * the headers.
   *
   * @return false if there was an error
   */
  private boolean writeHeaderBytesToFile(ByteBuffer buf) {
    try {
      FileChannel fc = m_mimeAccumulator.getFileOutputStream().getChannel();
      while(buf.hasRemaining()) {
        fc.write(buf);
      }
      return true;
    }
    catch(Exception ex) {
      m_logger.error("Error writing header bytes to file", ex);
      return false;
    }
  }

  /**
   * Method called when we are reading headers,
   * and encounter an exception parsing the headers.
   * If there is an error, the MIMEAccumulator
   * is not closed but any temp streams are.
   *
   * Returned buffer is flipped.
   */
  private ByteBuffer drainFileToByteBuffer() {
    FileInputStream fIn = null;
    try {
      fIn = new FileInputStream(m_mimeAccumulator.getFile());
      ByteBuffer buf = ByteBuffer.allocate((int) m_mimeAccumulator.getFile().length());
      FileChannel fc = fIn.getChannel();
      while(buf.hasRemaining()) {
        fc.read(buf);
      }
      fIn.close();
      buf.flip();
      return buf;
    }
    catch(Exception ex) {
      try {fIn.close();}catch(Exception ignore){}
      m_logger.error("Error draining headers trapped in file to buffer");
      return null;
    }
  }
  

  //============== Inner Class =====================
  
  /**
   * Class used to accumulate the temporary
   * file and associated members for
   * a MIMEMessage.
   */
  class MIMEAccumulator {
    private File m_file;
    private FileOutputStream m_fOut;
    private MIMEMessageHeaders m_headers;
    private MessageBoundaryScanner m_scanner;
    private FileMIMESource m_source;

    MIMEAccumulator(File file,
      FileOutputStream fOut) {
      m_file = file;
      m_fOut = fOut;
      m_scanner = new MessageBoundaryScanner();
    }
    MessageBoundaryScanner getScanner() {
      return m_scanner;
    }

    File getFile() {
      return m_file;
    }
    FileOutputStream getFileOutputStream() {
      return m_fOut;
    }
    void setHeaders(MIMEMessageHeaders headers) {
      m_headers = headers;
    }
    MIMEMessageHeaders getHeaders() {
      return m_headers;
    }
    void setFileSource(FileMIMESource source) {
      m_source = source;
    }
    FileMIMESource getFileSource() {
      return m_source;
    }

    /**
     * Closes the input stream, but leaves the file
     * assuming it'll be passed downstream.
     */
    void closeNormal() {
      try {m_fOut.close();}catch(Exception ignore){}
    }
    /**
     * Closes any streams and removes the file.
     */
    void closeFromError() {
      closeNormal();
      try {m_source.close();}catch(Exception ignore){}
      try {m_file.delete();}catch(Exception ignore){}
    }
    
  }

}