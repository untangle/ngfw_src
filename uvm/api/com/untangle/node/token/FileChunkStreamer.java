
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

package com.untangle.node.token;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Streams a file out as chunks.
 *
 */
public class FileChunkStreamer implements TokenStreamer
{
    private static final int CHUNK_SIZE = 1024;

    private final Logger logger = Logger.getLogger(FileChunkStreamer.class);

    private enum StreamState { BEGIN, FILE, END };

    private final File file;
    private final FileChannel channel;
    private final boolean closeWhenDone;
    private final List<Token> beginTokens;
    private final List<Token> endTokens;

    private StreamState state;
    private Iterator<Token> iterator = null;

    // constructors -----------------------------------------------------------

    private FileChunkStreamer(File file, FileChannel channel,
                              List<Token> beginTokens, List<Token> endTokens,
                              boolean closeWhenDone)
    {
        this.file = file;
        this.channel = channel;
        this.beginTokens = beginTokens;
        this.endTokens = endTokens;
        this.closeWhenDone = closeWhenDone;

        if (null == beginTokens) {
            state = StreamState.FILE;
        } else {
            iterator = this.beginTokens.iterator();
            state = StreamState.BEGIN;
        }
    }

    public FileChunkStreamer(File file, FileChannel channel,
                             Token beginToken, Token endToken,
                             boolean closeWhenDone)
    {
        this(file, channel,
             null == beginToken ? null : Arrays.asList(new Token[] { beginToken }),
             null == endToken ? null : Arrays.asList(new Token[] { endToken }),
             closeWhenDone);
    }


    public FileChunkStreamer(File file,
                             Token beginToken, Token endToken,
                             boolean closeWhenDone)
        throws IOException
    {
        this(file, new FileInputStream(file).getChannel(), beginToken,
             endToken, closeWhenDone);
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

    // TokenStreamer methods --------------------------------------------------

    @SuppressWarnings("fallthrough")
    public Token nextToken()
    {
        logger.debug("nextToken()");

        switch (state) {
        case BEGIN:
            if (iterator.hasNext()) {
                Token tok = iterator.next();
                logger.debug("returning: " + tok);
                return tok;
            } else {
                iterator = null;
                state = StreamState.FILE;
                logger.debug("falling through to FILE");
            }

        case FILE:
            ByteBuffer buf = ByteBuffer.allocate(CHUNK_SIZE);
            try {
                if (0 <= channel.read(buf)) {
                    buf.flip();
                    logger.debug("read chunk: " + buf.remaining());
                    return new Chunk(buf);
                } else {
                    channel.close();
                    file.delete();
                    iterator = null == endTokens ? null : endTokens.iterator();
                    state = StreamState.END;
                    logger.debug("falling through to END");
                }
            } catch (IOException exn) {
                logger.warn("could not read data", exn);
                return null;
            }

        case END:
            if (null != iterator && iterator.hasNext()) {
                logger.debug("returning iterator.next()");
                Token tok = iterator.next();
                logger.debug("returning token: " + tok);
                return tok;
            } else {
                logger.debug("returning null");
                return null;
            }

        default:
            throw new IllegalStateException("bad state: " + state);
        }
    }
}
