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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.ftp.FtpCommand;
import com.metavize.tran.ftp.FtpFunction;
import com.metavize.tran.ftp.FtpReply;
import com.metavize.tran.ftp.FtpStateMachine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.FileChunkStreamer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.TokenStreamerAdaptor;
import com.metavize.tran.util.TempFileFactory;
import org.apache.log4j.Logger;

class VirusFtpHandler extends FtpStateMachine
{
    /* XXX Should be from the same place as the HTTP constants */
    private static final int SCAN_COUNTER  = Transform.GENERIC_0_COUNTER;
    private static final int BLOCK_COUNTER = Transform.GENERIC_1_COUNTER;
    private static final int PASS_COUNTER  = Transform.GENERIC_2_COUNTER;


    private final VirusTransformImpl transform;
    private final boolean scanClient;
    private final boolean scanServer;

    private final Logger logger = Logger.getLogger(FtpStateMachine.class);
    private static final Logger eventLogger = MvvmContextFactory
        .context().eventLogger();

    private File file;
    private FileChannel inChannel;
    private FileChannel outChannel;
    private boolean c2s;
    private final TempFileFactory m_fileFactory;

    // constructors -----------------------------------------------------------

    VirusFtpHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        this.transform = transform;

        VirusSettings vs = transform.getVirusSettings();

        if (!session.isInbound()) { // outgoing
            scanClient = vs.getFtpOutbound().getScan();
            scanServer = vs.getFtpInbound().getScan();
        } else {
            scanClient = vs.getFtpInbound().getScan();
            scanServer = vs.getFtpOutbound().getScan();
        }

        m_fileFactory = new TempFileFactory(getPipeline());
    }

    // FtpStateMachine methods ------------------------------------------------

    @Override
    protected TokenResult doClientData(Chunk c) throws TokenException
    {
        if (scanClient) {
            logger.debug("doServerData()");

            if (null == file) {
                logger.debug("creating file for client");
                createFile();
                c2s = true;
            }

            Chunk outChunk = trickle(c.getData());

            return new TokenResult(null, new Token[] { outChunk });
        } else {
            return new TokenResult(null, new Token[] { c });
        }
    }

    @Override
    protected TokenResult doServerData(Chunk c) throws TokenException
    {
        if (scanServer) {
            logger.debug("doServerData()");

            if (null == file) {
                logger.debug("creating file for server");
                createFile();
                c2s = false;
            }

            Chunk outChunk = trickle(c.getData());

            return new TokenResult(new Token[] { outChunk }, null);
        } else {
            return new TokenResult(new Token[] { c }, null);
        }
    }

    @Override
    protected void doClientDataEnd() throws TokenException
    {
        logger.debug("doClientDataEnd()");

        if (scanClient && c2s && null != file) {
            try {
                outChannel.close();
            } catch (IOException exn) {
                logger.warn("could not close out channel");
            }

            logger.debug("c2s file: " + file);
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
        logger.debug("doServerDataEnd()");

        if (scanServer && !c2s && null != file) {
            try {
                outChannel.close();
            } catch (IOException exn) {
                logger.warn("could not close out channel", exn);
            }

            logger.debug("!c2s file: " + file);
            TCPStreamer ts = scan();
            if (null != ts) {
                getSession().beginClientStream(ts);
            }
            file = null;
        } else {
            getSession().shutdownClient();
        }
    }

    @Override
    protected TokenResult doCommand(FtpCommand command) throws TokenException
    {
        if (FtpFunction.REST == command.getFunction()
            && transform.getFtpDisableResume()) {
            FtpReply reply = FtpReply.makeReply(502, "Command not implemented.");
            return new TokenResult(new Token[] { reply }, null);
        } else {
            return new TokenResult(null, new Token[] { command });
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
            transform.incrementCount( SCAN_COUNTER );
            result = transform.getScanner().scanFile(file.getPath());
        } catch (Exception exn) {
            // Should never happen
            throw new TokenException("could not scan TokenException", exn);
        }

        /* XXX handle the case where result is null */

        eventLogger.info(new VirusLogEvent(getSession().id(), result, transform.getScanner().getVendorName()));

        if (result.isClean()) {
            transform.incrementCount( PASS_COUNTER );
            Pipeline p = getPipeline();
            TokenStreamer tokSt = new FileChunkStreamer
                (file, inChannel, null, EndMarker.MARKER, true);
            return new TokenStreamerAdaptor(p, tokSt);
        } else {
            transform.incrementCount( BLOCK_COUNTER );
            // Todo: Quarantine (for now, don't delete the file) XXX
            TCPSession s = getSession();
            s.shutdownClient();
            s.shutdownServer();
            return null;
        }
    }

    private void createFile() throws TokenException
    {
        try {
            file = m_fileFactory.createFile("ftp-virus");

            FileInputStream fis = new FileInputStream(file);
            inChannel = fis.getChannel();

            FileOutputStream fos = new FileOutputStream(file);
            outChannel = fos.getChannel();

        } catch (IOException exn) {
            throw new TokenException("could not create tmp file");
        }
    }
}
