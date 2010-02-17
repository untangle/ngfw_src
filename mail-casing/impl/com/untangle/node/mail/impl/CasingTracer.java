/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.event.TCPStreamer;


/**
 * Little class which is used <b>during development</b> to give a
 * tcpdump-like trace of conversations.
 */
public class CasingTracer {

    private static final String s_line_sep =
        System.getProperty("line.separator");

    private static ByteBuffer s_parsePrefix = ByteBuffer.wrap((
                                                                  s_line_sep +
                                                                  "============= PARSER =============" +
                                                                  s_line_sep).getBytes());

    private static ByteBuffer s_unParsePrefix = ByteBuffer.wrap((
                                                                    s_line_sep +
                                                                    "============ UNPARSER ============" +
                                                                    s_line_sep).getBytes());

    private static ByteBuffer s_end = ByteBuffer.wrap((
                                                          s_line_sep +
                                                          "============== END ===============" +
                                                          s_line_sep).getBytes());

    private final Logger m_logger = Logger.getLogger(CasingTracer.class);

    private FileOutputStream m_fOut;
    private FileChannel m_channel;
    private int m_closeCount = 0;

    /**
     * @param rootDir directory where all traces will be dumped
     * @param name the name of the trace file.  Will become<code>
     *        <i>name</i>_client|server.trace</code>
     * @param clientSide is this casing on the client side.
     */
    public CasingTracer(File rootDir,
                        String name,
                        boolean clientSide) {
        if(!rootDir.exists()) {
            rootDir.mkdirs();
        }
        try {
            File f = new File(rootDir, name + "_" + (clientSide?"client":"server") + ".trace");
            m_fOut = new FileOutputStream(f);
            m_channel = m_fOut.getChannel();
        }
        catch(Exception ex) {
            try {m_fOut.close();} catch(Exception ignore){}
            m_channel = null;
            m_logger.warn(ex);
        }
    }


    /**
     * Trace a parse message from the casing
     */
    public void traceParse(ByteBuffer buf) {
        traceWrite(s_parsePrefix, buf);
    }
    /**
     * Trace an unparse message from the casing
     */
    public void traceUnparse(ByteBuffer buf) {
        traceWrite(s_unParsePrefix, buf);
    }

    /**
     * Close ths trace.  Note that this happens automagically if
     * {@link #endSession endSession} is called twice.
     */
    public void close() {
        traceWrite(s_end, null);
        if(m_channel != null) {
            try {m_fOut.flush();}catch(Exception ignore){}
            try {m_channel.close();}catch(Exception ignore){}
            try {m_fOut.close();}catch(Exception ignore){}
            m_channel = null;
        }
    }

    /**
     * Called as the parser/unparser encounter the end of the session
     */
    public void endSession(boolean calledFromParser) {
        String closeMsg = s_line_sep +
            "============= CLOSE (" +
            (calledFromParser?"parser":"unparser") +
            ") =============" +
            s_line_sep;
        traceWrite(ByteBuffer.wrap(closeMsg.getBytes()), null);
        if(++m_closeCount > 1) {
            close();
        }
    }

    /**
     * Wraps a stream for tracing
     *
     * @param streamer the stream to wrap
     *
     * @return the streamer (wrapped for tracing)
     */
    public TCPStreamer wrapUnparseStreamerForTrace(TCPStreamer streamer) {
        return new TCPStreamerUnparseWrapper(streamer);
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
            m_logger.warn("Error writing to trace file", ex);
            m_channel = null;
        }
    }



    private class TCPStreamerUnparseWrapper
        implements TCPStreamer {
        private final TCPStreamer m_wrapped;
        TCPStreamerUnparseWrapper(TCPStreamer wrap) {
            m_wrapped = wrap;
        }
        public boolean closeWhenDone() {
            return m_wrapped.closeWhenDone();
        }
        public ByteBuffer nextChunk() {
            ByteBuffer ret = m_wrapped.nextChunk();
            if(ret != null) {
                traceUnparse(ret);
            }
            return ret;
        }
    }
}
