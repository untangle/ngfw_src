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

package com.metavize.tran.virus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.ftp.FtpStateMachine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.FileChunkStreamer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;

class VirusFtpHandler extends FtpStateMachine
{
    private final VirusTransformImpl transform;

    private File file;
    private FileChannel inChannel;
    private FileChannel outChannel;
    private boolean c2s;

    // constructors -----------------------------------------------------------

    VirusFtpHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        this.transform = transform;
    }

    // FtpStateMachine methods ------------------------------------------------

    @Override
    protected TokenResult doClientData(Chunk c) throws TokenException
    {
        if (null == file) {
            createFile();
            c2s = true;
        }

        Chunk outChunk = trickle(c.getData());

        return new TokenResult(null, new Token[] { outChunk });
    }

    @Override
    protected TokenResult doServerData(Chunk c) throws TokenException
    {
        if (null == file) {
            createFile();
            c2s = false;
        }

        Chunk outChunk = trickle(c.getData());

        return new TokenResult(new Token[] { outChunk }, null);
    }

    @Override
    protected void doClientDataEnd() throws TokenException
    {
        if (c2s) {
            TCPStreamer ts = scan();
            if (null != ts) {
                getSession().beginServerStream(ts);
            }
            file = null;
        } else {
            getSession().shutdownServer();
        }
    }

    @Override
    protected void doServerDataEnd() throws TokenException
    {
        if (!c2s) {
            TCPStreamer ts = scan();
            if (null != ts) {
                getSession().beginClientStream(ts);
            }
            file = null;
        } else {
            getSession().shutdownClient();
        }
    }

    // private methods --------------------------------------------------------

    private Chunk trickle(ByteBuffer b) throws TokenException
    {
        int l = b.remaining() * transform.getTricklePercent() / 100;

        try {
            while (b.hasRemaining()) {
                outChannel.write(b);
            }

            b.clear().limit(l);

            while (b.hasRemaining()) {
                inChannel.read(b);
            }
        } catch (IOException exn) {
            throw new TokenException("could not trickle", exn);
        }

        b.flip();

        return new Chunk(b);
    }

    private TCPStreamer scan() throws TokenException
    {
        VirusScannerResult result;
        try {
            result = transform.getScanner().scanFile(file.getPath());
        } catch (IOException exn) {
            throw new TokenException("could not scan TokenException", exn);
        } catch (InterruptedException exn) { // XXX deal with this in scanner
            throw new TokenException("interrupted while scanning", exn);
        }

        if (result.isClean()) {
            Pipeline p = getPipeline();
            return new FileChunkStreamer(p, file, inChannel, true);
        } else {
            TCPSession s = getSession();
            s.shutdownClient();
            s.shutdownServer();
            return null;
        }
    }

    private void createFile() throws TokenException
    {
        try {
            file = File.createTempFile("ftp-virus", null);
            file.deleteOnExit();

            FileInputStream fis = new FileInputStream(file);
            inChannel = fis.getChannel();

            FileOutputStream fos = new FileOutputStream(file);
            outChannel = fos.getChannel();

        } catch (IOException exn) {
            throw new TokenException("could not create tmp file");
        }
    }
}
