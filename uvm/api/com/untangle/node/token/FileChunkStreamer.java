/**
 * $Id$
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

    private FileChunkStreamer(File file, FileChannel channel, List<Token> beginTokens, List<Token> endTokens, boolean closeWhenDone)
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

    public FileChunkStreamer(File file, FileChannel channel, Token beginToken, Token endToken, boolean closeWhenDone)
    {
        this(file, channel,
             null == beginToken ? null : Arrays.asList(new Token[] { beginToken }),
             null == endToken ? null : Arrays.asList(new Token[] { endToken }),
             closeWhenDone);
    }

    public FileChunkStreamer(File file, Token beginToken, Token endToken, boolean closeWhenDone)
        throws IOException
    {
        this(file, new FileInputStream(file).getChannel(), beginToken, endToken, closeWhenDone);
    }

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

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
                    return new ChunkToken(buf);
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
