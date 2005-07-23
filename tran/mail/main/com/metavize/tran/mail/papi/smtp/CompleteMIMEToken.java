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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
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

  private final MIMEMessageHolder m_msgHolder;

  public CompleteMIMEToken(MIMEMessageHolder holder) {
    m_msgHolder = holder;
  }

  /**
   * Get the Holder of the MIMEMessage
   */
  public MIMEMessageHolder getHolder() {
    return m_msgHolder;
  }

  /**
   * Get a TokenStreamer for the contents of this
   * MIME Message
   *
   * @param pipeline the pipeline (needed for the Streamer stuff)
   * @return the TokenStreamer
   */
  public TokenStreamer toTokenStreamer(Pipeline pipeline) {
    return new MIMETokenStreamer(pipeline);
  }


  //TODO bscott How can we be assured we closed this stream/channel?
  private class MIMETokenStreamer
    implements TokenStreamer {

    private FileInputStream m_fos;
    private FileChannel m_channel;
    private final ByteBuffer m_readBuf = ByteBuffer.allocate(CHUNK_SZ);
    private ByteBufferByteStuffer m_bbbs = new ByteBufferByteStuffer();


    MIMETokenStreamer(final Pipeline pipeline) {
      try {
        File file = m_msgHolder.toFile(new FileFactory() {
          public File createFile(String name)
            throws IOException {
            return createFile();
          }

          public File createFile()
            throws IOException {
            return pipeline.mktemp();
          }
        });
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
    public Token nextToken() {
      if(m_channel == null) {
        return null;
      }

      try {
        m_readBuf.clear();
        ByteBuffer sinkBuf = ByteBuffer.allocate(CHUNK_SZ);
        int read = m_channel.read(m_readBuf);
        if(read > 0) {
          //TODO bscott the JavaDocs are unclear about "0"
          m_bbbs.transfer(m_readBuf, sinkBuf);
          return new Chunk(sinkBuf);
        }
        else {
          close();
          return new Chunk(m_bbbs.getLast(true));
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
