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

    public class VirusHttpState
    {
        private boolean scan = false;
        private long bufferingStart;
        private int outstanding;
        private int totalSize;
        private String extension = null;
        private String hostname = null;
        private File scanfile = null;
        private FileChannel outFile = null;
        private FileChannel inFile = null;
    }

    // constructors -----------------------------------------------------------

    VirusHttpHandler( VirusNodeImpl node )
    {
        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    public void handleNewSession( NodeTCPSession session )
    {
        super.handleNewSession( session );
        VirusHttpState state = new VirusHttpState();
        session.attach( state );
    }
    
    @Override
    protected RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken requestLine )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        String path = requestLine.getRequestUri().getPath();
        
        if ( path == null ) {
            state.extension = "";
        } else {
            int i = path.lastIndexOf('.');
            state.extension = (0 <= i && path.length() - 1 > i) ? path.substring(i + 1) : null;

            releaseRequest( session );
        }
        return requestLine;
    }

    @Override
    protected Header doRequestHeader( NodeTCPSession session, Header requestHeader )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        /* save hostname */
        if ( state.hostname == null ) {
            state.hostname = requestHeader.getValue("host");
        }
        if ( state.hostname == null ) {
            RequestLineToken requestLine = getRequestLine( session );
            if (requestLine != null)
                state.hostname = requestLine.getRequestUri().normalize().getHost();
        }
        
        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody( NodeTCPSession session, Chunk chunk )
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd( NodeTCPSession session ) { }

    @Override
    protected StatusLine doStatusLine( NodeTCPSession session, StatusLine statusLine )
    {
        return statusLine;
    }

    @Override
    protected Header doResponseHeader( NodeTCPSession session, Header header )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("doing response header");

        String reason = "";

        RequestLineToken rl = getResponseRequest( session );

        if (null == rl || HttpMethod.HEAD == rl.getMethod()) {
            logger.debug("CONTINUE or HEAD");
            state.scan = false;
        } else if ( ignoredHost( state.hostname ) ) {
            logger.debug("Ignoring downloads from: " + state.hostname);
            state.scan = false;
        } else if (matchesExtension( state.extension )) {
            logger.debug("matches extension");
            reason = state.extension;
            state.scan = true;
        } else {
            logger.debug("else...");
            String mimeType = header.getValue("content-type");

            state.scan = matchesMimeType(mimeType);
            if (logger.isDebugEnabled()) {
                logger.debug("content-type: " + mimeType + "matches mime-type: " + state.scan);
            }

            reason = mimeType;
        }

        if ( state.scan ) {
            state.bufferingStart = System.currentTimeMillis();
            state.outstanding = 0;
            state.totalSize = 0;
            setupFile( session, reason );
        } else {
            /* header.replaceField("accept-ranges", "none"); */
            releaseResponse( session );
        }

        return header;
    }

    @Override
    protected Chunk doResponseBody( NodeTCPSession session, Chunk chunk ) throws TokenException
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        return state.scan ? bufferOrTrickle( session, chunk ) : chunk;
    }

    @Override
    protected void doResponseBodyEnd( NodeTCPSession session )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        if (state.scan) {
            try {
                state.outFile.close();
            } catch (IOException exn) {
                logger.warn("could not close channel", exn);
            }
            scanFile( session );
            if ( getResponseMode( session ) == Mode.QUEUEING ) {
                logger.warn("still queueing after scanFile, buffering: " + getResponseMode( session ));
                releaseResponse( session );
            }
        } else {
            if ( getResponseMode( session ) == Mode.QUEUEING ) {
                logger.warn("still queueing, but not scanned");
                releaseResponse( session );
            }
        }
    }

    // private methods --------------------------------------------------------

    private void scanFile( NodeTCPSession session )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        VirusScannerResult result;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Scanning the file: " + state.scanfile);
            }
            node.incrementScanCount();
            result = node.getScanner().scanFile(state.scanfile);
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

        RequestLine requestLine = getResponseRequest( session ).getRequestLine();
        node.logEvent( new VirusHttpEvent(requestLine, result, node.getName()) );

        if (result.isClean()) {
            node.incrementPassCount();

            if ( getResponseMode( session ) == Mode.QUEUEING ) {
                releaseResponse( session );
                try { state.scanfile.delete(); } catch (Exception ignore) {}
            } else {
                streamClient( session, new FileChunkStreamer(state.scanfile, state.inFile, null, null, false) );
            }

        } else {
            node.incrementBlockCount();

            if ( getResponseMode( session ) == Mode.QUEUEING ) {
                RequestLineToken rl = getResponseRequest( session );
                String uri = null != rl ? rl.getRequestUri().toString() : "";
                String host = getResponseHost( session );
                logger.info("Virus found: " + host + uri + " = " + result.getVirusName());
                VirusBlockDetails bd = new VirusBlockDetails( host, uri, null, node.getName() );
                                                             
                String nonce = node.generateNonce(bd);
                Token[] response = node.generateResponse( nonce, session, uri );
                
                blockResponse( session, response );
            } else {
                logger.info("Virus found: " + result.getVirusName());
                session.shutdownClient();
                session.shutdownServer();
                session.release();
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

    private void setupFile( NodeTCPSession session, String reason )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("VIRUS: Scanning because of: " + reason);
        File fileBuf = null;

        try {
            fileBuf = File.createTempFile("VirusHttpHandler-", null);
            if (fileBuf != null)
                session.attachTempFile(fileBuf.getAbsolutePath());
            state.scanfile = fileBuf;

            if (logger.isDebugEnabled()) {
                logger.debug("VIRUS: Using temporary file: " + state.scanfile);
            }

            state.outFile = (new FileOutputStream(fileBuf)).getChannel();
            state.inFile = (new FileInputStream(fileBuf)).getChannel();
            state.scanfile = fileBuf;
            state.scan = true;
            
        } catch (IOException e) {
            logger.warn("Unable to create temporary file: " + e);
            state.scan = false;
            releaseResponse( session );
        } 
    }
    
    @Override
    public void handleFinalized( NodeTCPSession session )
    {
        session.cleanupTempFiles();
    }

    private Chunk bufferOrTrickle( NodeTCPSession session, Chunk chunk ) throws TokenException
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        ByteBuffer buf = chunk.getData();

        try {
            for (ByteBuffer bb = buf.duplicate(); bb.hasRemaining(); state.outFile.write(bb));
        } catch (IOException e) {
            logger.warn("Unable to write to buffer file: " + e);
            throw new TokenException(e);
        }

        state.outstanding += buf.remaining();
        state.totalSize += buf.remaining();

        if ( getResponseMode( session ) == Mode.QUEUEING ) {
            if (TIMEOUT > (System.currentTimeMillis() - state.bufferingStart) && SIZE_LIMIT > state.totalSize) {
                logger.debug("buffering");
                return chunk;
            } else {            /* switch to trickle mode */
                logger.debug("switching to trickling");
                try {
                    state.inFile.position(state.outstanding);
                } catch (IOException exn) {
                    logger.warn("could not change file pointer", exn);
                }
                state.outstanding = 0;
                releaseResponse( session );
                return chunk;
            }
        } else {                /* stay in trickle mode */
            logger.debug("trickling");
            if (MAX_SCAN_LIMIT < state.totalSize) {
                logger.debug("MAX_SCAN_LIMIT exceeded, not scanning");
                state.scan = false;

                streamClient( session, new FileChunkStreamer(state.scanfile, state.inFile, null, null, false) );

                return Chunk.EMPTY;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("continuing to trickle: " + state.totalSize);
                }
                Chunk c = trickle( session );
                return c;
            }
        }
    }

    private Chunk trickle( NodeTCPSession session ) throws TokenException
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("handleTokenTrickle()");

        int tricklePercent = node.getTricklePercent();
        int trickleLen = (state.outstanding * tricklePercent) / 100;
        ByteBuffer inbuf = ByteBuffer.allocate(trickleLen);

        inbuf.limit(trickleLen);

        try {
            for (; inbuf.hasRemaining(); state.inFile.read(inbuf));
        } catch (IOException e) {
            logger.warn("Unable to read from buffer file: " + e);
            throw new TokenException(e);
        }

        inbuf.flip();
        state.outstanding = 0;

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
        if (host == null){
            return false;
        }
        host = host.toLowerCase();
        Pattern p;

        for (Iterator<GenericRule> i = node.getSettings().getPassSites().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled() ){
                p = (Pattern) sr.attachment();
                if( null == p ){
                    try{
                        p = Pattern.compile( GlobUtil.globToRegex( sr.getString() ) );
                    }catch( Exception error ){
                        logger.error("Unable to compile passSite="+sr.getString());
                    }                    
                    sr.attach( p );
                }
                if( p.matcher( host ).matches() ){
                    return true;
                }
            }
        }
        return false;
    }
}
