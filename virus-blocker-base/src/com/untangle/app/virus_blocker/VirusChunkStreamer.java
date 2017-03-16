/**
 * $Id: VirusChunkStreamer.java 38197 2014-07-30 05:49:20Z dmorris $
 */
package com.untangle.app.virus_blocker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;

import org.apache.log4j.Logger;

/**
 * Streams a file out as chunks.
 * 
 */
public class VirusChunkStreamer implements TokenStreamer
{
    private static final int CHUNK_SIZE = 1024;

    private final Logger logger = Logger.getLogger(VirusChunkStreamer.class);

    private enum StreamState
    {
        BEGIN, FILE, END
    };

    private final File file;
    private final FileChannel channel;
    private final boolean closeWhenDone;
    private final List<Token> beginTokens;
    private final List<Token> endTokens;

    private VirusFileManager fileManager = null;
    private Iterator<Token> iterator = null;
    private StreamState state;

    private VirusChunkStreamer(File file, FileChannel channel, List<Token> beginTokens, List<Token> endTokens, boolean closeWhenDone)
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

    public VirusChunkStreamer(File file, FileChannel channel, Token beginToken, Token endToken, boolean closeWhenDone)
    {
        this(file, channel, null == beginToken ? null : Arrays.asList(new Token[] { beginToken }), null == endToken ? null : Arrays.asList(new Token[] { endToken }), closeWhenDone);
    }

    public VirusChunkStreamer(File file, Token beginToken, Token endToken, boolean closeWhenDone) throws IOException
    {
        this(file, new FileInputStream(file).getChannel(), beginToken, endToken, closeWhenDone);
    }

    public VirusChunkStreamer(VirusFileManager fileManager, Token beginToken, Token endToken, boolean closeWhenDone)
    {
        this(null, null, null == beginToken ? null : Arrays.asList(new Token[] { beginToken }), null == endToken ? null : Arrays.asList(new Token[] { endToken }), closeWhenDone);
        this.fileManager = fileManager;
    }

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }

    @SuppressWarnings("fallthrough")
    public Token nextToken()
    {
        logger.debug("nextToken()");

        switch (state)
        {
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
                int ret = localRead(buf);
                if (0 <= ret) {
                    buf.flip();
                    logger.debug("read chunk: " + buf.remaining() + " ret: " + ret);
                    return new ChunkToken(buf);
                } else {
                    if (fileManager == null) {
                        channel.close();
                        file.delete();
                    }
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

    int localRead(ByteBuffer buf) throws IOException
    {
        if (fileManager != null) return (fileManager.read(buf));
        else return channel.read(buf);
    }
}
