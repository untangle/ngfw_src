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

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;
import com.metavize.tran.mail.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import com.metavize.tran.mime.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;


/**
 * Class representing a Complete MIME message.
 * This will be issued if an upstream Transform
 * has buffered a complete message.
 */
public class CompleteMIMEToken
  extends MetadataToken {

  private static final int CHUNK_SZ = 1024*4;

  private final Logger m_logger =
    Logger.getLogger(CompleteMIMEToken.class);

  private final MIMEMessage m_msg;
  private final MessageInfo m_msgInfo;

  public CompleteMIMEToken(MIMEMessage msg,
    MessageInfo msgInfo) {
    m_msg = msg;
    m_msgInfo = msgInfo;
  }

  /**
   * Get the MIMEMessage
   */
  public MIMEMessage getMessage() {
    return m_msg;
  }

  public MessageInfo getMessageInfo() {
    return m_msgInfo;
  }

  /**
   * Get a TokenStreamer for the contents of this
   * MIME Message
   *
   * @param pipeline the pipeline (needed for the Streamer stuff)
   * @return the TokenStreamer
   */
  public TCPStreamer toTCPStreamer(Pipeline pipeline) {
    m_logger.debug("About to return a new MIMETCPStreamer");
    return new MIMETCPStreamer(pipeline);
  }


  //TODO bscott How can we be assured we closed this stream/channel?
  private class MIMETCPStreamer
    implements TCPStreamer {


    private FileInputStream m_fos;
    private FileChannel m_channel;
    private final ByteBuffer m_readBuf = ByteBuffer.allocate(CHUNK_SZ);
    private ByteBufferByteStuffer m_bbbs = new ByteBufferByteStuffer();


    MIMETCPStreamer(final Pipeline pipeline) {
      //TODO bscott Remove this debugging
      m_logger.debug("Created Complete MIME message streamer");
      try {
        File file = m_msg.toFile(new FileFactory() {
          public File createFile(String name)
            throws IOException {
            return createFile();
          }

          public File createFile()
            throws IOException {
            return pipeline.mktemp();
          }
        });
        m_logger.debug("File is of length: " + file.length());
        m_fos = new FileInputStream(file);
        m_channel = m_fos.getChannel();
      }
      catch(Exception ex) {
        m_logger.error(ex);
        close();
      }
    }

    private void close() {
      try {m_fos.close();}catch(Exception ignore){}
      m_fos = null;
      m_channel = null;
    }

    public boolean closeWhenDone() {
      return false;
    }
    
    public ByteBuffer nextChunk() {
      m_logger.debug("Next Chunk called");
      if(m_channel == null) {
        return null;
      }

      try {
        m_readBuf.clear();
        ByteBuffer sinkBuf = ByteBuffer.allocate(CHUNK_SZ);
        int read = m_channel.read(m_readBuf);
        if(read > 0) {
          //TODO bscott the JavaDocs are unclear about "0"
          m_logger.debug("Read a chunk of MIME from file of size: " + read);
          m_bbbs.transfer(m_readBuf, sinkBuf);
          m_logger.debug("Returning a ByteBuffer of size: " + sinkBuf.remaining());
          return sinkBuf;
        }
        else {
          m_logger.debug("No more MIME to read");
          close();
          ByteBuffer toWrap = m_bbbs.getLast(true);
          m_logger.debug("Final wrapped buffer of size: " + toWrap.remaining());
          return toWrap;
        }
      }
      catch(Exception ex) {
        m_logger.error(ex);
        close();
        return null;
      }

    }
  }
}
