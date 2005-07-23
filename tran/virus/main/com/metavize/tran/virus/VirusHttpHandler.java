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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.MimeTypeRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.http.HttpMethod;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.FileChunkStreamer;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

class VirusHttpHandler extends HttpStateMachine
{
    private static final int SCAN_COUNTER  = Transform.GENERIC_0_COUNTER;
    private static final int BLOCK_COUNTER = Transform.GENERIC_1_COUNTER;
    private static final int PASS_COUNTER  = Transform.GENERIC_2_COUNTER;

    private static final Logger logger = Logger
        .getLogger(VirusHttpHandler.class);
    private static final Logger eventLogger = MvvmContextFactory
        .context().eventLogger();

    private final VirusTransformImpl transform;
    private final List requestQueue = new LinkedList();

    private RequestLine responseRequest;
    private boolean scan;
    private String extension;
    private String fileName;
    private FileChannel outFile;
    private FileChannel inFile;
    private File file;

    // constructors -----------------------------------------------------------

    VirusHttpHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        this.transform = transform;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected TokenResult doRequestLine(RequestLine requestLine)
    {
        requestQueue.add(requestLine);

        this.scan = false;
        String path = requestLine.getRequestUri().getPath();

        int i = path.lastIndexOf('.');
        extension = (0 <= i && path.length() - 1 > i)
            ? path.substring(i + 1) : null;

        return new TokenResult(null, new Token[] { requestLine });
    }

    @Override
    protected TokenResult doRequestHeader(Header requestHeader)
    {
        logger.debug("got a request header");

        String range = requestHeader.getValue("byte-range");
        if (null == range || range.startsWith("0")) {
            logger.debug("passing because range: " + range);
            return new TokenResult(null, new Token[] { requestHeader });
        } else {
            logger.info("we dont accept ranges: " + range);
            // we dont accept ranges
            // XXX log this event
            // XXX make a response instead of shutting down
            getSession().shutdownServer();
            getSession().shutdownClient();
            return TokenResult.NONE;
        }
    }

    @Override
    protected TokenResult doRequestBody(Chunk chunk)
    {
        return new TokenResult(null, new Token[] { chunk });
    }

    @Override
    protected TokenResult doRequestBodyEnd(EndMarker endMarker)
    {
        return new TokenResult(null, new Token[] { endMarker });
    }

    @Override
    protected TokenResult doStatusLine(StatusLine statusLine)
    {
        responseRequest = 100 == statusLine.getStatusCode()
            ? null : (RequestLine)requestQueue.remove(0);

        return new TokenResult(new Token[] { statusLine }, null);
    }

    @Override
    protected TokenResult doResponseHeader(Header header)
    {
        logger.debug("doing response header");

        String reason = "";

        if (null == responseRequest
            || HttpMethod.HEAD == responseRequest.getMethod()) {
            logger.debug("CONTINUE or HEAD");
        } else if (matchesExtension(extension)) {
            logger.debug("matches extension");
            reason = extension;
            this.scan = true;
        } else {
            logger.debug("else...");
            String mimeType = header.getValue("content-type");
            logger.debug("content-type: " + mimeType);
            this.scan = matchesMimeType(mimeType);
            logger.debug("matches mime-type: " + scan);
            reason = mimeType;
        }

        if (scan) {
            setupFile( reason );
        }

        header.replaceField("accept-ranges", "none");
        return new TokenResult(new Token[] { header }, null);
    }

    @Override
    protected TokenResult doResponseBody(Chunk chunk)
    {
        if (scan) {
            return handleTokenTrickle(chunk);
        } else {
            logger.debug("passing through");
            return new TokenResult(new Token[] { chunk }, null);
        }
    }

    @Override
    protected TokenResult doResponseBodyEnd(EndMarker endMarker)
    {
        if (scan) {
            try {
                outFile.close();
            } catch (IOException exn) {
                logger.warn("could not close channel", exn);
            }
            return scanFile();
        } else {
            return new TokenResult(new Token[] { endMarker }, null);
        }
    }

    // TokenHandler methods ---------------------------------------------------

    @Override
    public TokenResult releaseFlush()
    {
        return TokenResult.NONE;
    }

    // private methods --------------------------------------------------------

    private TokenResult scanFile()
    {
        VirusScannerResult result;
        try {
            logger.debug("Scanning the file: " + fileName );
            transform.incrementCount( SCAN_COUNTER );
            result = transform.getScanner().scanFile(fileName);
        } catch (IOException e) {
            logger.error("Virus scan failed: "+ e);
            result = VirusScannerResult.ERROR;
        } catch (InterruptedException e) {
            logger.error("Virus scan failed: "+ e);
            result = VirusScannerResult.ERROR;
        }
        if (result == null) {
            logger.error("Virus scan failed: null");
            result = VirusScannerResult.ERROR;
        }

        eventLogger.info(new VirusHttpEvent(responseRequest, result, transform.getScanner().getVendorName()));

        if (result.isClean()) {
            transform.incrementCount(PASS_COUNTER, 1);

            if (result.isVirusCleaned()) {
                logger.info("Cleaned infected file");
            } else {
                logger.info("Clean");
            }

            FileChunkStreamer streamer = new FileChunkStreamer
                (file, inFile, null, EndMarker.MARKER, false);

            return new TokenResult(streamer, null);
        } else {
            logger.info("Virus found, killing session");
            // Todo: Quarantine (for now, don't delete the file) XXX
            transform.incrementCount(BLOCK_COUNTER, 1);
            getSession().shutdownClient();
            getSession().shutdownServer();

            return TokenResult.NONE;
        }
    }

    private boolean matchesExtension(String extension)
    {
        if (null == extension) { return false; }

        for (Iterator i = transform.getExtensions().iterator();
             i.hasNext(); ) {
            StringRule sr = (StringRule)i.next();
            if (sr.isLive() && sr.getString().equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesMimeType(String mimeType)
    {
        int longestMatch = 0;
        boolean isLive = false;
        String match = "";

        if (null == mimeType) {
            return false;
        }

        /*
         * XXX This is inefficient, but typically there are only a
         * few rules in this list.
         */
        for (Iterator i = transform.getHttpMimeTypes().iterator();
             i.hasNext(); ) {

            MimeTypeRule mtr = (MimeTypeRule)i.next();
            String currentMt = mtr.getMimeType().getType();

            /* Skip all of the shorter or equal mimetypes */
            if ( currentMt.length() <= longestMatch ) {
                continue;
            }

            if ( mtr.getMimeType().matches( mimeType )) {
                /* Exact match, break */
                if ( currentMt.length() == mimeType.length() ) {
                    isLive = mtr.isLive();
                    match = currentMt;
                    break;
                }

                /* This must be a wildcard match, don't include the
                 * '*' in the length of the match
                 */
                longestMatch = currentMt.length() - 1;
                isLive = mtr.isLive();
                match = currentMt;
            }
        }

        if ( logger.isDebugEnabled())
            logger.debug( "Mapped: " + mimeType + " to: '" + match + "' scan: "+ isLive );

        return isLive;
    }

    private void setupFile(String reason)
    {
        logger.info("VIRUS: Scanning because of: " + reason);
        String localFileName = null;

        try {
            localFileName= makeFileName(getSession());
            File fileBuf = File.createTempFile(localFileName,null);

            this.fileName = fileBuf.getAbsolutePath();

            if ( logger.isDebugEnabled())
                logger.debug( "VIRUS: Using temporary file: " + this.fileName );

            this.outFile = (new FileOutputStream(fileBuf)).getChannel();
            this.inFile = (new FileInputStream(fileBuf)).getChannel();
            this.file = fileBuf;
            this.scan = true;
        } catch (IOException e) {
            logger.warn("Unable to create file: " + localFileName + "\n" + e);
            this.scan = false;
        }
    }

    private TokenResult handleTokenTrickle(Chunk chunk)
    {
        logger.debug("handleTokenTrickle()");

        ByteBuffer buff =  chunk.getData();
        int tricklePercent = transform.getTricklePercent();
        int trickleLen = (buff.remaining() * tricklePercent)/100;
        ByteBuffer inbuf = ByteBuffer.allocate(trickleLen);
        ByteBuffer[] bufs = new ByteBuffer[1];

        bufs[0] = inbuf;

        try {
            ByteBuffer bb = buff.duplicate();
            while (bb.remaining()>0) {
                this.outFile.write(bb);
            }
        } catch (IOException e) {
            logger.warn("Unable to write to buffer file: " + e);
            return TokenResult.NONE;
        }

        inbuf.limit(trickleLen);

        try {
            if (this.inFile.read(inbuf)<trickleLen) {
                logger.warn("Read unsuccessful/truncated");
            }
        } catch (IOException e) {
            logger.warn("Unable to read from buffer file: " + e);
            return TokenResult.NONE;
        }

        inbuf.flip();

        Chunk ck = new Chunk(inbuf);

        //logger.debug("VIRUS: Flushing \"" + (char)inbuf.get(0) + "\" to Receiver");
        return new TokenResult(new Token[] { ck }, null);
    }

    private String makeFileName (TCPSession sess)
    {
        return "filebuf-" + sess.clientAddr().getHostAddress().toString()
            + ":" + Integer.toString(sess.clientPort()) + "-"
            + sess.serverAddr().getHostAddress().toString()
            + ":" + Integer.toString(sess.serverPort());
    }
}
