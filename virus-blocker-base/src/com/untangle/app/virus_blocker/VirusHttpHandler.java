/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.app.http.HttpMethod;
import com.untangle.app.http.HttpEventHandler;
import com.untangle.app.http.RequestLine;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.StatusLine;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.GlobUtil;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;

/**
 * Virus handler for HTTP.
 */
class VirusHttpHandler extends HttpEventHandler
{
    /**
     * BUFFER_TIMEOUT configures the maximum amount of time a file will be
     * buffered to disk before it starts trickling the file to the user
     */
    private static final int BUFFER_TIMEOUT = 1000 * 4; // 4 seconds

    /**
     * BUFFER_SIZE_LIMIT configures the maximum amount of bytes a file will be
     * buffered to disk before it starts trickling the file to the user
     */
    private static final int BUFFER_SIZE_LIMIT = 1024 * 1024 * 20; //20 Meg

    /**
     * MAX_SCAN_LIMIT configures the maximum size of any file to be scanned If
     * the file is larger it is assumed to be clean
     */
    private static final int MAX_SCAN_LIMIT = 200 * 1024 * 1024; //200 Meg

    /**
     * CACHE_EXPIRATION_MS configures the amount of time a positive entry is
     * stored in the cache This is so that we don't permanently block false
     * positives in case we cache one
     */
    private static final long CACHE_EXPIRATION_MS = 1000 * 60 * 60; //1 hour

    /**
     * MEMORY_SIZE_LIMIT configures the size of the file we hold back from the
     * client while we calculate the MD5 checksum when we are operating in
     * memory buffering mode where there is no disk file
     */
    private static final int MEMORY_SIZE_LIMIT = 1024 * 16; //16 Kilobytes

    private final Logger logger = Logger.getLogger(getClass());

    private final VirusBlockerBaseApp app;

    private VirusUrlCache<VirusUrlCacheKey, VirusUrlCacheEntry> urlCache = new VirusUrlCache<VirusUrlCacheKey, VirusUrlCacheEntry>();

    /**
     * Holds the Virus scanner HTTP state
     */
    protected class VirusHttpState extends VirusBlockerState
    {
        private VirusFileManager fileManager = null;
        private boolean memoryMode = false;
        private boolean scan = false;
        private long bufferingStart;
        private int outstanding;
        private int totalSize;
    }

    /**
     * Constructor
     * 
     * @param app
     *        The virus blocker base application
     */
    protected VirusHttpHandler(VirusBlockerBaseApp app)
    {
        this.app = app;
    }

    /**
     * Handle new TCP sessions
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleTCPNewSession(AppTCPSession session)
    {
        super.handleTCPNewSession(session);
        VirusHttpState state = new VirusHttpState();
        session.attach(state);
    }

    /**
     * Handle the request line
     * 
     * @param session
     *        The session
     * @param requestLine
     *        The request line
     * @return The request line
     */
    @Override
    protected RequestLineToken doRequestLine(AppTCPSession session, RequestLineToken requestLine)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();

        return requestLine;
    }

    /**
     * Handle the request header
     * 
     * @param session
     *        The session
     * @param requestHeader
     *        The request header
     * @return The request header
     */
    @Override
    protected HeaderToken doRequestHeader(AppTCPSession session, HeaderToken requestHeader)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        /* save host */
        if (state.host == null) {
            state.host = requestHeader.getValue("host");
        }
        if (state.host == null) {
            RequestLineToken requestLine = getRequestLine(session);
            if (requestLine != null) state.host = requestLine.getRequestUri().normalize().getHost();
        }
        RequestLineToken requestLineToken = getRequestLine(session);
        if (requestLineToken != null) state.uri = requestLineToken.getRequestUri().normalize().getPath();
        else state.uri = "";

        if (!ignoredHost(state.host)) {
            String virusName = lookupCache(state.host, state.uri);
            if (virusName != null) {
                // increment both scan and block count
                app.incrementScanCount();
                app.incrementBlockCount();

                VirusBlockDetails bd = new VirusBlockDetails(state.host, state.uri, null, app.getName());
                Token[] response = app.generateResponse(bd, session, state.uri, requestHeader);
                blockRequest(session, response);

                RequestLine requestLine = getRequestLine(session).getRequestLine();
                app.logEvent(new VirusHttpEvent(requestLine, session.sessionEvent(), false, virusName, app.getName()));

                return requestHeader;
            }
        }

        releaseRequest(session);
        return requestHeader;
    }

    /**
     * Handle the request body
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk
     * @return The chunk
     */
    @Override
    protected ChunkToken doRequestBody(AppTCPSession session, ChunkToken chunk)
    {
        return chunk;
    }

    /**
     * Handle the request body end
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doRequestBodyEnd(AppTCPSession session)
    {
    }

    /**
     * Handle the status line
     * 
     * @param session
     *        The session
     * @param statusLine
     *        The status line
     * @return The status line
     */
    @Override
    protected StatusLine doStatusLine(AppTCPSession session, StatusLine statusLine)
    {
        return statusLine;
    }

    /**
     * Handle the response header
     * 
     * @param session
     *        The session
     * @param header
     *        The header
     * @return The header
     */
    @Override
    protected HeaderToken doResponseHeader(AppTCPSession session, HeaderToken header)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        logger.debug("doing response header");

        RequestLineToken rl = getResponseRequest(session);

        String contentDisposition = header.getValue("content-disposition");
        logger.debug("content-disposition: " + contentDisposition);
        String mimeType = header.getValue("content-type");
        logger.debug("content-type: " + mimeType);
        String contentDispositionFilename = (String) session.globalAttachment(AppSession.KEY_HTTP_RESPONSE_FILE_NAME);
        logger.debug("content-disposition filename: " + contentDispositionFilename);

        if (rl == null || rl.getMethod() == HttpMethod.HEAD) {
            logger.debug("CONTINUE or HEAD");
            state.scan = false;
        } else if (ignoredHost(state.host)) {
            logger.debug("ignoring content from: " + state.host);
            state.scan = false;
        } else if (matchesExtension(state.uri)) {
            logger.debug("matches uri-extension");
            state.scan = true;
        } else if (matchesExtension(contentDispositionFilename)) {
            logger.debug("matched file-extension");
            state.scan = true;
        } else if (matchesMimeType(mimeType)) {
            logger.debug("matched mime-type");
            state.scan = true;
        } else {
            state.scan = false;
        }

        if (state.scan) {
            state.bufferingStart = System.currentTimeMillis();
            state.outstanding = 0;
            state.totalSize = 0;
            setupFile(session);
        } else {
            /* header.replaceField("accept-ranges", "none"); */
            releaseResponse(session);
        }

        return header;
    }

    /**
     * Handle the response body
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk
     * @return The chunk
     */
    @Override
    protected ChunkToken doResponseBody(AppTCPSession session, ChunkToken chunk)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        return state.scan ? bufferOrTrickle(session, chunk) : chunk;
    }

    /**
     * Handle the response body end
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doResponseBodyEnd(AppTCPSession session)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        if (state.scan) {
            scanFile(session);
            if (getResponseMode(session) == Mode.QUEUEING) {
                logger.warn("still queueing after scanFile, buffering: " + getResponseMode(session));
                releaseResponse(session);
            }
        } else {
            if (getResponseMode(session) == Mode.QUEUEING) {
                logger.warn("still queueing, but not scanned");
                releaseResponse(session);
            }
        }
    }

    /**
     * Scan a file
     * 
     * @param session
     *        The session
     */
    private void scanFile(AppTCPSession session)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        VirusScannerResult result;
        try {
            logger.debug("Scanning the HTTP file: " + state.fileManager.getFileDisplayName());
            app.incrementScanCount();
            state.fileHash = state.fileManager.getFileHash();
            result = app.getScanner().scanFile(state.fileManager.getFileObject(), session);
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

        RequestLine requestLine = getResponseRequest(session).getRequestLine();
        app.logEvent(new VirusHttpEvent(requestLine, session.sessionEvent(), result.isClean(), result.getVirusName(), app.getName()));

        if (result.isClean()) {
            app.incrementPassCount();

            if (getResponseMode(session) == Mode.QUEUEING) {
                releaseResponse(session);
                state.fileManager.delete();
            } else {
                streamClient(session, new VirusChunkStreamer(state.fileManager, null, null, false));
            }

        } else {
            app.incrementBlockCount();

            addCache(state.host, state.uri, result.getVirusName());

            if (getResponseMode(session) == Mode.QUEUEING) {
                RequestLineToken rl = getResponseRequest(session);
                String uri = null != rl ? rl.getRequestUri().toString() : "";
                String host = getResponseHost(session);
                logger.info("Virus found: " + host + uri + " = " + result.getVirusName());
                VirusBlockDetails bd = new VirusBlockDetails(host, uri, null, app.getName());

                Token[] response = app.generateResponse(bd, session, uri);

                blockResponse(session, response);
            } else {
                logger.info("Virus found: " + result.getVirusName());
                session.shutdownClient();
                session.shutdownServer();
                session.release();
            }
        }
    }

    /**
     * Check for extension match
     * 
     * @param filename
     *        The file
     * @return True for match, otherwise false
     */
    private boolean matchesExtension(String filename)
    {
        if (filename == null) return false;

        for (Iterator<GenericRule> i = app.getSettings().getHttpFileExtensions().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled() && filename.toLowerCase().endsWith(sr.getString().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for mime type match
     * 
     * @param mimeType
     *        The mime type
     * @return True for match, otherwise false
     */
    private boolean matchesMimeType(String mimeType)
    {
        int longestMatch = 0;
        boolean isLive = false;
        String match = "";

        if (mimeType == null) {
            return false;
        }

        for (Iterator<GenericRule> i = app.getSettings().getHttpMimeTypes().iterator(); i.hasNext();) {
            GenericRule rule = i.next();
            Object regexO = rule.attachment();
            Pattern regex = null;

            /**
             * If the regex is not attached to the rule, compile a new one and
             * attach it Otherwise just use the regex already compiled and
             * attached to the rule
             */
            if (regexO == null || !(regexO instanceof Pattern)) {
                String re = GlobUtil.urlGlobToRegex(rule.getString());

                logger.debug("Compile  rule: " + re);
                regex = Pattern.compile(re);
                rule.attach(regex);
            } else {
                regex = (Pattern) regexO;
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

    /**
     * Setup a temporary file to hold the content to be scanned
     * 
     * @param session
     *        The session
     */
    private void setupFile(AppTCPSession session)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();

        // start with the memory only flag in the settings
        state.memoryMode = app.getSettings().getForceMemoryMode();

        // if the file scanner is not installed we MUST use memory mode
        if (app.isFileScannerAvailable() == false) {
            state.memoryMode = true;
        }

        try {
            state.fileManager = new VirusFileManager(state.memoryMode, "VirusHttpHandler-");
            state.scan = true;
            if (state.memoryMode == false) {
                session.attachTempFile(state.fileManager.getTempFileAbsolutePath());
            }
        } catch (Exception exn) {
            logger.warn("Unable to initialize file manager: ", exn);
        }
    }

    /**
     * Handle TCP finalized
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleTCPFinalized(AppTCPSession session)
    {
        session.cleanupTempFiles();
    }

    /**
     * Buffer or trickle content data
     * 
     * @param session
     *        The session
     * @param chunk
     *        The chunk
     * @return The chunk
     */
    private ChunkToken bufferOrTrickle(AppTCPSession session, ChunkToken chunk)
    {
        VirusHttpState state = (VirusHttpState) session.attachment();
        ByteBuffer buf = chunk.getData();

        for (ByteBuffer bb = buf.duplicate(); bb.hasRemaining(); state.fileManager.write(bb))
            ;

        state.outstanding += buf.remaining();
        state.totalSize += buf.remaining();

        if (getResponseMode(session) == Mode.QUEUEING) {
            long elaspsedTime = System.currentTimeMillis() - state.bufferingStart;

            // switch to trickle mode at different points when buffering to disk or buffering to memory
            int triggerSize = (state.memoryMode ? MEMORY_SIZE_LIMIT : BUFFER_SIZE_LIMIT);

            if ((elaspsedTime < BUFFER_TIMEOUT) && (state.totalSize < triggerSize)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("continue buffering... " + elaspsedTime + "ms and " + state.totalSize + " bytes.");
                }
                return chunk;
            } else {
                /**
                 * switch to trickle mode if we reached the time or size limits
                 * or if memory buffering mode is active
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("switch to trickling after " + elaspsedTime + "ms and " + state.totalSize + " bytes.");
                }
                state.fileManager.position(state.outstanding);
                state.outstanding = 0;
                releaseResponse(session);
                return chunk;
            }
        } else { /* stay in trickle mode */
            if (MAX_SCAN_LIMIT < state.totalSize) {
                logger.debug("MAX_SCAN_LIMIT exceeded, not scanning");
                state.scan = false;
                streamClient(session, new VirusChunkStreamer(state.fileManager, null, null, false));
                return ChunkToken.EMPTY;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("continuing to trickle: " + state.totalSize);
                }
                ChunkToken c = trickle(session);
                return c;
            }
        }
    }

    /**
     * Trickle content data
     * 
     * @param session
     *        The session
     * @return The chunk
     */
    private ChunkToken trickle(AppTCPSession session)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("handleTokenTrickle()");
        }

        int trickleLen = 0;

        VirusHttpState state = (VirusHttpState) session.attachment();
        if (state.memoryMode == true) {
            /**
             * when buffering to memory without a disk file let the file buffer
             * up to our limit and then we start sending data
             */
            int counter = state.fileManager.getMemoryCounter();
            if (counter > MEMORY_SIZE_LIMIT) trickleLen = (counter - MEMORY_SIZE_LIMIT);
            else trickleLen = 0;
        } else {
            /**
             * when buffering to a disk file use configured percentage
             */
            int tricklePercent = app.getTricklePercent();
            trickleLen = (state.outstanding * tricklePercent) / 100;
        }

        ByteBuffer inbuf = ByteBuffer.allocate(trickleLen);
        inbuf.limit(trickleLen);

        for (; inbuf.hasRemaining(); state.fileManager.read(inbuf))
            ;

        inbuf.flip();
        state.outstanding = 0;

        return new ChunkToken(inbuf);
    }

    /**
     * Get the keep-alive flag from the header
     * 
     * @param header
     *        The header
     * @return The keep-alive flag
     */
    @SuppressWarnings("unused")
    private boolean isPersistent(HeaderToken header)
    {
        String con = header.getValue("connection");
        return con == null ? false : con.equalsIgnoreCase("keep-alive");
    }

    /**
     * Check a host against the ignore list
     * 
     * @param host
     *        The host to check
     * @return True if ignored, otherwise false
     */
    private boolean ignoredHost(String host)
    {
        if (host == null) {
            return false;
        }
        host = host.toLowerCase();
        Pattern p;

        for (Iterator<GenericRule> i = app.getSettings().getPassSites().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled()) {
                p = (Pattern) sr.attachment();
                if (p == null) {
                    try {
                        p = Pattern.compile(GlobUtil.globToRegex(sr.getString()));
                    } catch (Exception error) {
                        logger.error("Unable to compile passSite=" + sr.getString());
                    }
                    sr.attach(p);
                }
                if ( (p != null) && p.matcher(host).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add a cache result to the url cache Only positive results can be cached
     * 
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @param virusName
     *        The virus name
     */
    private void addCache(String host, String uri, String virusName)
    {
        VirusUrlCacheKey key = new VirusUrlCacheKey();
        key.host = host;
        key.uri = uri;

        VirusUrlCacheEntry entry = new VirusUrlCacheEntry();
        entry.virusName = virusName;
        entry.creationTimeMillis = System.currentTimeMillis();

        logger.info("urlCache add: " + host + uri + " = " + virusName);
        urlCache.put(key, entry);
    }

    /**
     * 
     * Lookup a virus result from the url cache returns the virus name if its a
     * known virus or null
     * 
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @return The cached virus name or null if not known
     */
    private String lookupCache(String host, String uri)
    {
        VirusUrlCacheKey key = new VirusUrlCacheKey();
        key.host = host;
        key.uri = uri;

        VirusUrlCacheEntry entry = urlCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.creationTimeMillis - System.currentTimeMillis() > CACHE_EXPIRATION_MS) {
            urlCache.remove(key);
            return null;
        }

        logger.info("urlCache hit: " + host + uri + " -> " + entry.virusName);
        return entry.virusName;
    }

    /**
     * Virus Url Cache stores the most recent positive URLs so we can quickly
     * block them on subsequent attemps The key is VirusUrlCacheKey, the entries
     * are the virus name
     */
    @SuppressWarnings("serial")
    private class VirusUrlCache<K, V> extends LinkedHashMap<K, V>
    {
        private static final int MAX_ENTRIES = 500;

        /**
         * Removed the oldest entry
         * 
         * @param eldest
         * @return Result
         */
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
        {
            return size() > MAX_ENTRIES;
        }
    }

    /**
     * Virus URL Cache Key
     */
    @SuppressWarnings("serial")
    private class VirusUrlCacheKey implements Serializable
    {
        protected String host;
        protected String uri;

        /**
         * Compare with an object
         * 
         * @param o2
         *        The object for comparison
         * @return True if equal, otherwise false
         */
        public boolean equals(Object o2)
        {
            if (!(o2 instanceof VirusUrlCacheKey)) {
                return false;
            }
            VirusUrlCacheKey o = (VirusUrlCacheKey) o2;
            if (!(o.host == null ? this.host == null : o.host.equals(this.host))) {
                return false;
            }
            if (!(o.uri == null ? this.uri == null : o.uri.equals(this.uri))) {
                return false;
            }
            return true;
        }

        /**
         * Get the hash code
         * 
         * @return The hash code
         */
        public int hashCode()
        {
            int hashCode1 = (host == null ? 1 : host.hashCode());
            int hashCode2 = (uri == null ? 3 : uri.hashCode());
            return hashCode1 + hashCode2;
        }

    }

    /**
     * Virus URL Cache Entry
     */
    @SuppressWarnings("serial")
    private class VirusUrlCacheEntry implements Serializable
    {
        protected String virusName;
        protected long creationTimeMillis;
    }

    /**
     * Clear the event handler cache
     */
    protected void clearEventHandlerCache()
    {
        logger.debug("urlCache clear: removing " + urlCache.size() + " items");
        urlCache.clear();
    }
}
