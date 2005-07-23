
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.mail.internet.ContentType;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.MessageFile;
import com.metavize.tran.mail.MimeBoundary;
import com.metavize.tran.mail.Rfc822Header;
import com.metavize.tran.mail.SmtpCommand;
import com.metavize.tran.mail.SmtpReply;
import com.metavize.tran.mail.SmtpStateMachine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.FileChunkStreamer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.token.header.IllegalFieldException;
import org.apache.log4j.Logger;

public class VirusSmtpHandler extends SmtpStateMachine
{
    private final Logger logger = Logger.getLogger(VirusSmtpHandler.class);

    private static final int CUTOFF = 1 << 20;

    // XXX for demo purposes, use settings to do something more appropriate
    private static final String REJECT_MESSAGE
        = "this attachment contained a virus\r\n";

    private final VirusTransformImpl transform;
    private final Pipeline pipeline;

    private Rfc822Header header = null;
    private File bodyFile = null;
    private FileChannel bodyChannel;
    private int outputSize = 0;

    // constructors -----------------------------------------------------------

    VirusSmtpHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        this.transform = transform;

        this.pipeline = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());

    }

    // SmtpStateMachine methods -----------------------------------------------

    protected TokenResult doSmtpCommand(SmtpCommand cmd)
    {
        return new TokenResult(null, new Token[] { cmd });
    }

    protected TokenResult doMessageHeader(Rfc822Header header)
        throws TokenException
    {
        if (hasScannableBody(header)) {
            try {
                this.header = header;
                this.bodyFile = pipeline.mktemp();
                this.bodyChannel = new FileOutputStream(bodyFile).getChannel();
                outputSize = 0;
            } catch (IOException exn) {
                throw new TokenException("could not create tmpfile", exn);
            }

            return TokenResult.NONE;
        } else {
            this.header = null;
            this.bodyFile = null;
            this.bodyChannel = null;
            return new TokenResult(null, new Token[] { header });
        }
    }

    protected TokenResult doBodyChunk(Chunk chunk) throws TokenException
    {
        if (null != bodyFile) {
            return writeFile(chunk);
        } else {
            return new TokenResult(null, new Token[] { chunk });
        }
    }

    protected TokenResult doPreamble(Chunk chunk)
    {
        return new TokenResult(null, new Token[] { chunk });
    }

    protected TokenResult doBoundary(MimeBoundary boundary, boolean end)
        throws TokenException
    {
        if (null != bodyFile) {
            return scanFile(boundary);
        } else {
            logger.debug("not streaming result");
            return new TokenResult(null, new Token[] { boundary });
        }
    }

    protected TokenResult doMultipartHeader(Rfc822Header header)
        throws TokenException
    {
        if (hasScannableBody(header)) {
            try {
                this.header = header;
                this.bodyFile = pipeline.mktemp();
                this.bodyChannel = new FileOutputStream(bodyFile).getChannel();
                outputSize = 0;
            } catch (IOException exn) {
                throw new TokenException("could not create tmpfile", exn);
            }

            return TokenResult.NONE;
        } else {
            this.header = null;
            this.bodyFile = null;
            this.bodyChannel = null;
            return new TokenResult(null, new Token[] { header });
        }
    }

    protected TokenResult doMultipartBody(Chunk chunk)
        throws TokenException
    {
        if (null != bodyFile) {
            return writeFile(chunk);
        } else {
            return new TokenResult(null, new Token[] { chunk });
        }
    }

    protected TokenResult doEpilogue(Chunk chunk)
    {
        return new TokenResult(null, new Token[] { chunk });
    }

    protected TokenResult doMessageFile(MessageFile messageFile)
    {
        return new TokenResult(null, new Token[] { messageFile });
    }

    protected TokenResult doMessageEnd(EndMarker endMarker)
        throws TokenException
    {
        if (null != bodyFile) {
            return scanFile(endMarker);
        } else {
            return new TokenResult(null, new Token[] { endMarker });
        }
    }

    protected TokenResult doSmtpReply(SmtpReply reply)
    {
        return new TokenResult(new Token[] { reply }, null);
    }

    // XXX make this check settings
    private boolean hasScannableBody(Rfc822Header header)
    {
        ContentType contentType = header.getContentType();
        if (null != contentType) {
            String pType = contentType.getPrimaryType();
            // XXX check settings for applicable mime types
            return pType.equals("application");
        } else {
            return false;
        }
    }

    private TokenResult writeFile(Chunk chunk) throws TokenException
    {
        ByteBuffer buf = chunk.getData();
        outputSize += buf.remaining();
        try {
            for ( ; buf.hasRemaining(); bodyChannel.write(buf));
        } catch (IOException exn) {
            throw new TokenException("exception writing chunk", exn);
        }

        if (CUTOFF < outputSize) {
            return streamFile(null);
        } else {
            return TokenResult.NONE;
        }
    }

    private TokenResult streamFile(Token endToken) throws TokenException
    {
        logger.debug("passing through header and body");
        try {
            FileChunkStreamer streamer = new FileChunkStreamer
                (bodyFile, header, endToken, false);

            this.header = null;
            this.bodyFile = null;
            this.bodyChannel = null;
            this.outputSize = 0;

            return new TokenResult(null, streamer);
        } catch (IOException exn) {
            throw new TokenException("couldn't stream", exn);
        }
    }

    private TokenResult scanFile(Token endToken) throws TokenException
    {
        logger.debug("streamResult");
        try {
            bodyChannel.close();
            VirusScannerResult result = transform.getScanner()
                .scanFile(bodyFile.getPath());
            if (result.isClean()) {
                return streamFile(endToken);
            } else {
                this.header = null;
                this.bodyFile = null;
                this.bodyChannel = null;
                this.outputSize = 0;

                Rfc822Header header = new Rfc822Header();
                try {
                    header.setContentType("text/plain");
                } catch (IllegalFieldException exn) {
                    throw new IllegalStateException("should never happen");
                }

                ByteBuffer buf = ByteBuffer.allocate(REJECT_MESSAGE.length());
                buf.put(REJECT_MESSAGE.getBytes());
                buf.flip();
                Chunk c = new Chunk(buf);

                logger.debug("new header and body");
                Token[] tokens = new Token[] { header, c, endToken };
                return new TokenResult(null, tokens);
            }
        } catch (IOException exn) {
            throw new TokenException("could not scan TokenException", exn);
        } catch (InterruptedException exn) { // XXX deal with this in scanner
            throw new TokenException("interrupted while scanning", exn);
        }
    }
}
