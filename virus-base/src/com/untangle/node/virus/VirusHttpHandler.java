/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.node.http.HttpMethod;
import com.untangle.node.http.HttpEventHandler;
import com.untangle.node.http.RequestLine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.FileChunkStreamer;
import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.GlobUtil;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Virus handler for HTTP.
 */
class VirusHttpHandler extends HttpEventHandler
{
    /**
     * BUFFER_TIMEOUT configures the maximum amount of time a file will be buffered to disk
     * before it starts trickling the file to the user
     */
    private static final int BUFFER_TIMEOUT = 1000*4; // 4 seconds

    /**
     * BUFFER_SIZE_LIMIT configures the maximum amount of bytes a file will be buffered to disk
     * before it starts trickling the file to the user
     */
    private static final int BUFFER_SIZE_LIMIT = 1024*1024*20; //20 Meg

    /**
     * MAX_SCAN_LIMIT configures the maximum size of any file to be scanned
     * If the file is larger it is assumed to be clean
     */
    private static final int MAX_SCAN_LIMIT = 200*1024*1024; //200 Meg

    /**
     * CACHE_EXPIRATION_MS configures the amount of time a positive entry is stored in the cache
     * This is so that we don't permanently block false positives in case we cache one
     */
    private static final long CACHE_EXPIRATION_MS = 1000*60*60; //1 hour
    
    private final Logger logger = Logger.getLogger(getClass());
    
    private final VirusNodeImpl node;

    private VirusUrlCache<VirusUrlCacheKey,VirusUrlCacheEntry> urlCache = new VirusUrlCache<VirusUrlCacheKey,VirusUrlCacheEntry>();
    
    protected class VirusHttpState
    {
        private boolean scan = false;
        private long bufferingStart;
        private int outstanding;
        private int totalSize;
        private String filenameContentDisposition = null; /* The content disposition filename extension */
        private String host = null;
        private String uri = null;
        private File scanfile = null;
        private FileChannel outFile = null;
        private FileChannel inFile = null;
    }

    protected VirusHttpHandler( VirusNodeImpl node )
    {
        this.node = node;
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        super.handleTCPNewSession( session );
        VirusHttpState state = new VirusHttpState();
        session.attach( state );
    }
    
    @Override
    protected RequestLineToken doRequestLine( NodeTCPSession session, RequestLineToken requestLine )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        
        return requestLine;
    }

    @Override
    protected HeaderToken doRequestHeader( NodeTCPSession session, HeaderToken requestHeader )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        /* save host */
        if ( state.host == null ) {
            state.host = requestHeader.getValue("host");
        }
        if ( state.host == null ) {
            RequestLineToken requestLine = getRequestLine( session );
            if (requestLine != null)
                state.host = requestLine.getRequestUri().normalize().getHost();
        }
        if ( state.uri == null ) {
            RequestLineToken requestLine = getRequestLine( session );
            if (requestLine != null)
                state.uri = requestLine.getRequestUri().normalize().getPath();
        }

        if ( ! ignoredHost( state.host ) ) {
            String virusName = lookupCache( state.host, state.uri );
            if ( virusName != null ) {
                VirusBlockDetails bd = new VirusBlockDetails( state.host, state.uri, null, node.getName() );
                String nonce = node.generateNonce(bd);
                Token[] response = node.generateResponse( nonce, session, state.uri, requestHeader );
                blockRequest( session, response );

                RequestLine requestLine = getRequestLine( session ).getRequestLine();
                node.logEvent( new VirusHttpEvent( requestLine, false, virusName, node.getName()) );

                return requestHeader;
            }
        }

        releaseRequest( session );
        return requestHeader;
    }

    @Override
    protected ChunkToken doRequestBody( NodeTCPSession session, ChunkToken chunk )
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
    protected HeaderToken doResponseHeader( NodeTCPSession session, HeaderToken header )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("doing response header");

        state.filenameContentDisposition = findContentDispositionFilename( header );

        RequestLineToken rl = getResponseRequest( session );
        
        if ( rl == null || HttpMethod.HEAD == rl.getMethod() ) {
            logger.debug("CONTINUE or HEAD");
            state.scan = false;
        } else if ( ignoredHost( state.host ) ) {
            logger.debug("Ignoring downloads from: " + state.host);
            state.scan = false;
        } else if ( matchesExtension( state.uri ) ) {
            logger.debug("matches uri");
            state.scan = true;
        } else if ( matchesExtension( state.filenameContentDisposition ) ) {
            logger.debug("matches filenameContentDisposition");
            state.scan = true;
        } else {
            String mimeType = header.getValue("content-type");

            state.scan = matchesMimeType(mimeType);
            if (logger.isDebugEnabled()) {
                logger.debug("content-type: " + mimeType + "matches mime-type: " + state.scan);
            }
        }

        if ( state.scan ) {
            state.bufferingStart = System.currentTimeMillis();
            state.outstanding = 0;
            state.totalSize = 0;
            setupFile( session );
        } else {
            /* header.replaceField("accept-ranges", "none"); */
            releaseResponse( session );
        }

        return header;
    }

    @Override
    protected ChunkToken doResponseBody( NodeTCPSession session, ChunkToken chunk )
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
        node.logEvent( new VirusHttpEvent(requestLine, result.isClean(), result.getVirusName(), node.getName()) );

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

            addCache( state.host, state.uri, result.getVirusName() );
            
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

    private boolean matchesExtension( String filename )
    {
        if ( filename == null )
            return false;

        for (Iterator<GenericRule> i = node.getSettings().getHttpFileExtensions().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled() && filename.toLowerCase().endsWith(sr.getString().toLowerCase())) {
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

        if ( mimeType == null ) {
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

    private void setupFile( NodeTCPSession session )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("VIRUS: Scanning");
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
    public void handleTCPFinalized( NodeTCPSession session )
    {
        session.cleanupTempFiles();
    }

    private ChunkToken bufferOrTrickle( NodeTCPSession session, ChunkToken chunk )
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        ByteBuffer buf = chunk.getData();

        try {
            for (ByteBuffer bb = buf.duplicate(); bb.hasRemaining(); state.outFile.write(bb));
        } catch (IOException e) {
            logger.warn("Unable to write to buffer file: " + e);
            throw new RuntimeException(e);
        }

        state.outstanding += buf.remaining();
        state.totalSize += buf.remaining();

        if ( getResponseMode( session ) == Mode.QUEUEING ) {
            long elaspsedTime = System.currentTimeMillis() - state.bufferingStart;
            
            if ( elaspsedTime < BUFFER_TIMEOUT && state.totalSize < BUFFER_SIZE_LIMIT ) {
                if (logger.isDebugEnabled())
                    logger.debug("continue buffering... " + elaspsedTime + "ms and " + state.totalSize + " bytes.");
                return chunk;
            } else {
                /**
                 * switch to trickle mode
                 */
                if (logger.isDebugEnabled())
                    logger.debug("switch to trickling after " + elaspsedTime + "ms and " + state.totalSize + " bytes.");
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

                return ChunkToken.EMPTY;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("continuing to trickle: " + state.totalSize);
                }
                ChunkToken c = trickle( session );
                return c;
            }
        }
    }

    private ChunkToken trickle( NodeTCPSession session )
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
            throw new RuntimeException(e);
        }

        inbuf.flip();
        state.outstanding = 0;

        return new ChunkToken(inbuf);
    }

    @SuppressWarnings("unused")
    private boolean isPersistent(HeaderToken header)
    {
        String con = header.getValue("connection");
        return con == null ? false : con.equalsIgnoreCase("keep-alive");
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
                if( p == null ) {
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

    /**
     * Add a cache result to the url cache
     * Only positive results can be cached
     */
    private void addCache( String host, String uri, String virusName )
    {
        VirusUrlCacheKey key = new VirusUrlCacheKey();
        key.host = host;
        key.uri = uri;

        VirusUrlCacheEntry entry = new VirusUrlCacheEntry();
        entry.virusName = virusName;
        entry.creationTimeMillis = System.currentTimeMillis();
        
        logger.info( "urlCache add: " + host + uri + " = " + virusName);
        urlCache.put( key, entry );
    }

    /**
     * Lookup a virus result from the url cache
     * returns the virus name if its a known virus or null
     */
    private String lookupCache( String host, String uri )
    {
        VirusUrlCacheKey key = new VirusUrlCacheKey();
        key.host = host;
        key.uri = uri;

        VirusUrlCacheEntry entry = urlCache.get( key );
        if ( entry == null ) {
            return null;
        }
        if ( entry.creationTimeMillis - System.currentTimeMillis() > CACHE_EXPIRATION_MS ) {
            urlCache.remove( key );
            return null;
        }

        logger.info( "urlCache hit: " + host + uri + " -> " + entry.virusName );
        return entry.virusName;
    }
   
    
    /**
     * Virus Url Cache stores the most recent positive URLs so we can quickly block them on subsequent attemps
     * The key is VirusUrlCacheKey, the entries are the virus name
     */
    @SuppressWarnings("serial")
    private class VirusUrlCache<K,V> extends LinkedHashMap<K,V>
    {
        private static final int MAX_ENTRIES = 500;

        protected boolean removeEldestEntry(Map.Entry<K,V> eldest)
        {
            return size() > MAX_ENTRIES;
        }
    }

    @SuppressWarnings("serial")
    private class VirusUrlCacheKey implements Serializable
    {
        protected String host;
        protected String uri;

        public boolean equals( Object o2 )
        {
            if ( ! ( o2 instanceof VirusUrlCacheKey ) ) {
                return false;
            }
            VirusUrlCacheKey o = (VirusUrlCacheKey) o2;
            if ( ! ( o.host == null ? this.host == null : o.host.equals(this.host) ) ) {
                return false;
            }
            if ( ! ( o.uri == null ? this.uri == null : o.uri.equals(this.uri) ) ) {
                return false;
            }
            return true;
        }

        public int hashCode()
        {
            int hashCode1 = ( host == null ? 1 : host.hashCode() );
            int hashCode2 = ( uri == null ? 3 : uri.hashCode() );
            return hashCode1 + hashCode2;
        }
        
    }

    @SuppressWarnings("serial")
    private class VirusUrlCacheEntry implements Serializable
    {
        protected String virusName;
        protected long creationTimeMillis;
    }

}

