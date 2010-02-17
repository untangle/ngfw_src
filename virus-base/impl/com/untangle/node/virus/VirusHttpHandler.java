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

package com.untangle.node.virus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.FileChunkStreamer;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Virus handler for HTTP.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class VirusHttpHandler extends HttpStateMachine
{
    // make configurable
    private static final int TIMEOUT = 30000;
    private static final int SIZE_LIMIT = 256000;
    private static final int MAX_SCAN_LIMIT = 200000000;

    private static final String BLOCK_MESSAGE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>Virus Blocked!</b></center>"
        + "<p>This site blocked because it contained a virus</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    private final Logger logger = Logger.getLogger(getClass());

    private final String vendor;
    private final VirusNodeImpl node;

    private boolean scan;
    private long bufferingStart;
    private int outstanding;
    private int totalSize;
    private String extension;
    private File scanfile;
    private FileChannel outFile;
    private FileChannel inFile;
    private File file;

    // constructors -----------------------------------------------------------

    VirusHttpHandler(TCPSession session, VirusNodeImpl node)
    {
        super(session);

        this.node = node;
        this.vendor = node.getScanner().getVendorName();
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        String path = requestLine.getRequestUri().getPath();
        if (null == path) {
            extension = "";
        } else {
            int i = path.lastIndexOf('.');
            extension = (0 <= i && path.length() - 1 > i)
                ? path.substring(i + 1) : null;

            releaseRequest();
        }
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        logger.debug("got a request header");

        if (null != requestHeader.getValue("range")
            && node.getHttpDisableResume()) {
            String ua = requestHeader.getValue("user-agent");
            if (!ua.startsWith("Microsoft BITS")) {
                requestHeader.removeField("range");
            }
        }

        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() { }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        logger.debug("doing response header");

        String reason = "";

        RequestLineToken rl = getResponseRequest();

        if (null == rl || HttpMethod.HEAD == rl.getMethod()) {
            logger.debug("CONTINUE or HEAD");
            this.scan = false;
        } else if (matchesExtension(extension)) {
            logger.debug("matches extension");
            reason = extension;
            this.scan = true;
        } else {
            logger.debug("else...");
            String mimeType = header.getValue("content-type");

            this.scan = matchesMimeType(mimeType);
            if (logger.isDebugEnabled()) {
                logger.debug("content-type: " + mimeType
                             + "matches mime-type: " + scan);
            }

            reason = mimeType;
        }

        if (scan) {
            bufferingStart = System.currentTimeMillis();
            outstanding = 0;
            totalSize = 0;
            setupFile(reason);
        } else {
            header.replaceField("accept-ranges", "none");
            releaseResponse();
        }

        return header;
    }

    @Override
    protected Chunk doResponseBody(Chunk chunk) throws TokenException
    {
        return scan ? bufferOrTrickle(chunk) : chunk;
    }

    @Override
    protected void doResponseBodyEnd()
    {
        if (scan) {
            try {
                outFile.close();
            } catch (IOException exn) {
                logger.warn("could not close channel", exn);
            }
            scanFile();
            if (getResponseMode() == Mode.QUEUEING) {
                logger.warn("still queueing after scanFile, buffering: "
                            + getResponseMode());
                releaseResponse();
            }
        } else {
            if (getResponseMode() == Mode.QUEUEING) {
                logger.warn("still queueing, but not scanned");
                releaseResponse();
            }
        }
    }

    // private methods --------------------------------------------------------

    private void scanFile()
    {
        VirusScannerResult result;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Scanning the file: " + scanfile);
            }
            node.incrementScanCount();
            result = node.getScanner().scanFile(scanfile);
        } catch (Exception e) {
            // Should never happen
            logger.error("Virus scan failed: ", e);
            result = VirusScannerResult.ERROR;
        }

        if (result == null) {
            // Should never happen
            logger.error("Virus scan failed: null");
            result = VirusScannerResult.ERROR;
        }

        RequestLine requestLine = getResponseRequest().getRequestLine();
        node.log(new VirusHttpEvent(requestLine, result,  vendor));

        if (result.isClean()) {
            node.incrementPassCount();

            if (result.isVirusCleaned()) {
                logger.info("Cleaned infected file");
            } else {
                logger.info("Clean");
            }

            if (Mode.QUEUEING == getResponseMode()) {
                releaseResponse();
            } else {
                preStream(new FileChunkStreamer(file, inFile, null, null, false));
            }

        } else {
            logger.info("Virus found, killing session");
            // Todo: Quarantine (for now, don't delete the file) XXX
            node.incrementBlockCount();

            if (Mode.QUEUEING == getResponseMode()) {
                RequestLineToken rl = getResponseRequest();
                String uri = null != rl ? rl.getRequestUri().toString() : "";
                String host = getResponseHost();
                VirusBlockDetails bd = new VirusBlockDetails(host,uri,null,this.vendor);
                                                             
                String nonce = node.generateNonce(bd);
                boolean p = isRequestPersistent();
                TCPSession sess = getSession();
                Token[] response = node.generateResponse(nonce, sess, uri, p);
                blockResponse(response);
            } else {
                TCPSession s = getSession();
                s.shutdownClient();
                s.shutdownServer();
                s.release();
            }
        }
    }

    private Token[] blockMessage()
    {
        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");

        RequestLineToken rl = getResponseRequest();
        String uri = null != rl ? rl.getRequestUri().toString() : "";
        String host = getResponseHost();

        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();
        String message = String.format(BLOCK_MESSAGE, host, uri, bs.getContactHtml());

        Header h = new Header();
        h.addField("Content-Length", Integer.toString(message.length()));
        h.addField("Content-Type", "text/html");
        h.addField("Connection", isResponsePersistent() ? "Keep-Alive" : "Close");

        ByteBuffer buf = ByteBuffer.allocate(message.length());
        buf.put(message.getBytes());
        buf.flip();
        Chunk c = new Chunk(buf);

        return new Token[] { sl, h, c, EndMarker.MARKER };
    }

    private boolean matchesExtension(String extension)
    {
        if (null == extension) { return false; }

        for (Iterator i = node.getExtensions().iterator();
             i.hasNext();) {
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
         * XXX This is inefficient, but typically there are only a few
         * rules in this list.
         */
        for (Iterator i = node.getHttpMimeTypes().iterator(); i.hasNext();) {

            MimeTypeRule mtr = (MimeTypeRule)i.next();
            String currentMt = mtr.getMimeType().getType();

            /* Skip all of the shorter or equal mimetypes */
            if (currentMt.length() <= longestMatch) {
                continue;
            }

            if (mtr.getMimeType().matches(mimeType)) {
                /* Exact match, break */
                if (currentMt.length() == mimeType.length()) {
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

        if (logger.isDebugEnabled()) {
            logger.debug("Mapped: " + mimeType + " to: '" + match
                         + "' scan: "+ isLive);
        }

        return isLive;
    }

    private void setupFile(String reason)
    {
        logger.info("VIRUS: Scanning because of: " + reason);
        try {
            TempFileFactory tff = new TempFileFactory(getPipeline());
            File fileBuf = tff.createFile("http-virus");

            this.scanfile = fileBuf;

            if (logger.isDebugEnabled()) {
                logger.debug("VIRUS: Using temporary file: " + this.scanfile);
            }

            this.outFile = (new FileOutputStream(fileBuf)).getChannel();
            this.inFile = (new FileInputStream(fileBuf)).getChannel();
            this.file = fileBuf;
            this.scan = true;
        } catch (IOException e) {
            logger.warn("Unable to create temporary file: " + e);
            this.scan = false;
            releaseResponse();
        }
    }

    private Chunk bufferOrTrickle(Chunk chunk) throws TokenException
    {
        ByteBuffer buf = chunk.getData();

        try {
            for (ByteBuffer bb = buf.duplicate(); bb.hasRemaining(); outFile.write(bb));
        } catch (IOException e) {
            logger.warn("Unable to write to buffer file: " + e);
            throw new TokenException(e);
        }

        outstanding += buf.remaining();
        totalSize += buf.remaining();

        if (Mode.QUEUEING == getResponseMode()) {
            if (TIMEOUT > (System.currentTimeMillis() - bufferingStart)
                && SIZE_LIMIT > totalSize) {
                logger.debug("buffering");
                return chunk;
            } else {            /* switch to trickle mode */
                logger.debug("switching to trickling");
                try {
                    inFile.position(outstanding);
                } catch (IOException exn) {
                    logger.warn("could not change file pointer", exn);
                }
                outstanding = 0;
                releaseResponse();
                return chunk;
            }
        } else {                /* stay in trickle mode */
            logger.debug("trickling");
            if (MAX_SCAN_LIMIT < totalSize) {
                logger.debug("MAX_SCAN_LIMIT exceeded, not scanning");
                scan = false;
                FileChunkStreamer streamer = new FileChunkStreamer
                    (file, inFile, null, null, false);
                preStream(streamer);

                return Chunk.EMPTY;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("continuing to trickle: " + totalSize);
                }
                Chunk c = trickle();
                return c;
            }
        }
    }

    private Chunk trickle() throws TokenException
    {
        logger.debug("handleTokenTrickle()");

        int tricklePercent = node.getTricklePercent();
        int trickleLen = (outstanding * tricklePercent) / 100;
        ByteBuffer inbuf = ByteBuffer.allocate(trickleLen);

        inbuf.limit(trickleLen);

        try {
            for (; inbuf.hasRemaining(); inFile.read(inbuf));
        } catch (IOException e) {
            logger.warn("Unable to read from buffer file: " + e);
            throw new TokenException(e);
        }

        inbuf.flip();
        outstanding = 0;

        return new Chunk(inbuf);
    }

    private boolean isPersistent(Header header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }
}
