/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.event.TCPStreamer;


/**
 * Class which acts as a TCPStreamer, using the contents
 * of a MIMEMessage.  Users should not directly have to
 * use this (it is public only because of subclassing/packaging
 * reasons).
 * <br><br>
 * Note that the MIMEMessage is first converted to a file
 * during the constructor, so {@link #getFileLength getFileLength}
 * will return the total number of bytes to be returned.  If an error
 * is encountered converting the MIMEMessage to a file, the length
 * of bytes returned is 0 (and the error is logged).
 */
public class MIMETCPStreamer
    implements TCPStreamer {

    private final int m_chunkSz;
    private FileInputStream m_fos;
    private FileChannel m_channel;
    private final Logger m_logger =
        Logger.getLogger(MIMETCPStreamer.class);
    private final boolean m_disposeWhenComplete;
    private boolean m_closed = false;
    private long m_fileLength;
    private final MIMEMessage m_msg;

    /**
     * Construct a new MIMETCPStreamer
     *
     * @param msg the MIMEMessage
     * @param pipeline the pipeline (for creating temp files)
     * @param readChunkSz the size of the read buffer for the file
     * @param disposeWhenComplete if true, the MIMEMessage's
     *        {@link com.untangle.node.mime.MIMEPart#dispose dispose} method will be called
     *        when streaming is complete or an error is encountered.
     */
    public MIMETCPStreamer(MIMEMessage msg,
                           final Pipeline pipeline,
                           int readChunkSz,
                           boolean disposeWhenComplete) {

        m_msg = msg;
        m_chunkSz = readChunkSz;
        m_disposeWhenComplete = disposeWhenComplete;
        //TODO bscott Remove this debugging
        m_logger.debug("Created Complete MIME message streamer");
        try {
            File file = m_msg.toFile(new TempFileFactory(pipeline));
            m_logger.debug("File is of length: " + file.length());
            m_fos = new FileInputStream(file);
            m_channel = m_fos.getChannel();
            m_fileLength = file.length();
        }
        catch(Exception ex) {
            m_fileLength = 0;
            m_logger.error(ex);
            close();
        }
    }

    /**
     * Get the length of the file.
     */
    public long getFileLength() {
        return m_fileLength;
    }

    private void close() {
        if(m_closed) {
            return;
        }
        m_closed = true;
        try {m_fos.close();}catch(Exception ignore){}
        m_fos = null;
        m_channel = null;
        if(m_disposeWhenComplete) {
            m_msg.dispose();
            m_logger.debug("Disposing of MIME file");
        }
    }

    public boolean closeWhenDone() {
        return false;
    }

    /**
     * Method creates a new read buffer.  By default, a new buffer
     * of length "readChunkSz" is allocated and used.  This method may
     * be overidden by subclasses, which do not intend to return this
     * buffer to the ultimate caller of {@link #nextChunk nextChunk}.  In
     * such a case, a read buffer may be re-used (of the contents of the
     * read are going to be modified anyway.
     */
    protected ByteBuffer createReadBuf() {
        return ByteBuffer.allocate(m_chunkSz);
    }

    public ByteBuffer nextChunk() {
        m_logger.debug("Next Chunk called");
        if(m_channel == null) {
            return null;
        }
        try {
            ByteBuffer ret = createReadBuf();
            if(m_channel.read(ret) < 0) {
                m_logger.debug("Reached end-of-file");
                close();
                return null;
            }
            else {
                ret.flip();
                return ret;
            }
        }
        catch(Exception ex) {
            m_logger.error(ex);
            close();
            return null;
        }
    }
}
