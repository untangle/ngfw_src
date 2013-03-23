/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.FileChunkStreamer;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.util.GlobUtil;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Virus handler for HTTP.
 */
class VirusHttpHandler extends HttpStateMachine
{
    // make configurable
    private static final int TIMEOUT = 30000;
    private static final int SIZE_LIMIT = 256000;
    private static final int MAX_SCAN_LIMIT = 200000000;

    private final Logger logger = Logger.getLogger(getClass());

    private final VirusNodeImpl node;

    private boolean scan;
    private long bufferingStart;
    private int outstanding;
    private int totalSize;
    private String extension;
    private String hostname;
    private File scanfile;
    private FileChannel outFile;
    private FileChannel inFile;
    private File file;

    // constructors -----------------------------------------------------------

    VirusHttpHandler(NodeTCPSession session, VirusNodeImpl node)
    {
        super(session);

        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        String path = requestLine.getRequestUri().getPath();
        
        if ( path == null ) {
            this.extension = "";
        } else {
            int i = path.lastIndexOf('.');
            this.extension = (0 <= i && path.length() - 1 > i) ? path.substring(i + 1) : null;

            releaseRequest();
        }
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        /* save hostname */
        if ( this.hostname == null ) {
            this.hostname = requestHeader.getValue("host");
        }
        if ( this.hostname == null ) {
            RequestLineToken requestLine = getRequestLine();
            if (requestLine != null)
                this.hostname = requestLine.getRequestUri().normalize().getHost();
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
        } else if ( ignoredHost( this.hostname ) ) {
            logger.debug("Ignoring downloads from: " + this.hostname);
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
                logger.debug("content-type: " + mimeType + "matches mime-type: " + scan);
            }

            reason = mimeType;
        }

        if (scan) {
            bufferingStart = System.currentTimeMillis();
            outstanding = 0;
            totalSize = 0;
            setupFile(reason);
        } else {
            /* header.replaceField("accept-ranges", "none"); */
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
                logger.warn("still queueing after scanFile, buffering: " + getResponseMode());
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
        node.logEvent( new VirusHttpEvent(requestLine, result, node.getName()) );

        if (result.isClean()) {
            node.incrementPassCount();

            if (result.isVirusCleaned()) {
                logger.info("Cleaned infected file:" + scanfile);
            } else {
                logger.debug("Clean");
            }

            if (Mode.QUEUEING == getResponseMode()) {
                releaseResponse();
            } else {
                preStream(new FileChunkStreamer(file, inFile, null, null, false));
            }

        } else {
            node.incrementBlockCount();

            if (Mode.QUEUEING == getResponseMode()) {
                RequestLineToken rl = getResponseRequest();
                String uri = null != rl ? rl.getRequestUri().toString() : "";
                String host = getResponseHost();
                logger.info("Virus found: " + host + uri + " = " + result.getVirusName());
                VirusBlockDetails bd = new VirusBlockDetails( host, uri, null, node.getName() );
                                                             
                String nonce = node.generateNonce(bd);
                NodeTCPSession sess = getSession();

                //bug #9164 - always close connection after writing redirect despite if the connection is persistent
                //Token[] response = node.generateResponse(nonce, sess, uri, isRequestPersistent());
                Token[] response = node.generateResponse(nonce, sess, uri, false);
                
                blockResponse(response);
            } else {
                logger.info("Virus found: " + result.getVirusName());
                NodeTCPSession s = getSession();
                s.shutdownClient();
                s.shutdownServer();
                s.release();
            }
        }
    }

    private boolean matchesExtension(String extension)
    {
        if (null == extension) { return false; }

        for (Iterator<GenericRule> i = node.getSettings().getHttpFileExtensions().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled() && sr.getString().equalsIgnoreCase(extension)) {
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

        for (Iterator<GenericRule> i = node.getSettings().getHttpMimeTypes().iterator(); i.hasNext();) {
            GenericRule rule = i.next();
            Object  regexO = rule.attachment();
            Pattern regex  = null;

            /**
             * If the regex is not attached to the rule, compile a new one and attach it
             * Otherwise just use the regex already compiled and attached to the rule
             */
            if (regexO == null || !(regexO instanceof Pattern)) {
                String re = GlobUtil.urlGlobToRegex(rule.getString());

                logger.debug("Compile  rule: " + re );
                regex = Pattern.compile(re);
                rule.attach(regex);
            } else {
                regex = (Pattern)regexO;
            }

            /**
             * Check the match
             */
            try {
                if (regex.matcher(mimeType).matches()) {
                    return rule.getEnabled();
                } 
            } catch (PatternSyntaxException e) {
                logger.error("findMatchRule: ** invalid pattern '" + regex + "'");		
            }
        }

        return false;
    }

    private void setupFile(String reason)
    {
        logger.debug("VIRUS: Scanning because of: " + reason);
        try {
            File fileBuf = File.createTempFile("VirusHttpHandler-", null);

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

    @SuppressWarnings("unused")
	private boolean isPersistent(Header header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }

    private boolean ignoredHost( String host )
    {
        if (host == null)
            return false;
        host = host.toLowerCase();
        
        if ("download.windowsupdate.com".equals(host))
            return true;
        if ("windowsupdate.microsoft.com".equals(host))
            return true;
        if ("update.microsoft.com".equals(host))
            return true;

        return false;
    }
}
