/**
 * $Id$
 */
package com.untangle.node.http;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Header;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenStreamer;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.node.util.UserAgentString;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * An HTTP <code>Parser</code>.
 */
public class HttpParser extends AbstractParser
{
    private final Logger logger = Logger.getLogger(HttpParser.class);

    private static final byte SP = ' ';
    private static final byte HT = '\t';
    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private static final int NO_BODY = 0;
    private static final int CLOSE_ENCODING = 1;
    private static final int CHUNKED_ENCODING = 2;
    private static final int CONTENT_LENGTH_ENCODING = 3;

    // longest allowable token or text:
    private static final int TIMEOUT = 30000;

    private static final int PRE_FIRST_LINE_STATE = 0;
    private static final int FIRST_LINE_STATE = 1;
    private static final int ACCUMULATE_HEADER_STATE = 2;
    private static final int HEADER_STATE = 3;
    private static final int CLOSED_BODY_STATE = 4;
    private static final int CONTENT_LENGTH_BODY_STATE = 5;
    private static final int CHUNK_LENGTH_STATE = 6;
    private static final int CHUNK_BODY_STATE  = 7;
    private static final int CHUNK_END_STATE  = 8;
    private static final int LAST_CHUNK_STATE = 9;
    private static final int END_MARKER_STATE = 10;

    private final HttpCasing casing;
    private byte[] buf;
    private final int maxHeader;
    private final boolean blockLongHeaders;
    private final int maxUri;
    private final int maxRequestLine;
    private final boolean blockLongUris;
    private final String sessStr;

    private RequestLineToken requestLineToken;
    private StatusLine statusLine;
    private Header header;

    private int state;
    private int transferEncoding;
    private int contentLength; /* counts down content-length and chunks */
    private int lengthCounter; /* counts up to final */

    // constructors -----------------------------------------------------------

    protected HttpParser(NodeTCPSession session, boolean clientSide, HttpCasing casing)
    {
        super(session, clientSide);
        HttpNodeImpl node = casing.getNode();
        HttpSettings settings = node.getHttpSettings();
        this.maxHeader = settings.getMaxHeaderLength();
        this.blockLongHeaders = settings.getBlockLongHeaders();
        this.maxUri = settings.getMaxUriLength();
        this.maxRequestLine = maxUri + 13;
        this.blockLongUris = settings.getBlockLongUris();
        this.casing = casing;
        this.sessStr = "HttpParser" + (clientSide ? " client-side " : " server-side ");

        // This is now initialized just before we need it and removed afterwards
        this.buf = null;

        lineBuffering(true);
    }

    // Parser methods ------------------------------------------------------

    public ParseResult parse(ByteBuffer b) throws ParseException
    {
        cancelTimer();

        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + "parsing chunk: " + b);
        }
        List<Token> tokenList = new LinkedList<Token>();

        boolean done = false;
        while (!done) {
            switch (state) {
            case PRE_FIRST_LINE_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in PRE_FIRST_LINE_STATE");
                    }

                    lengthCounter = 0;

                    if (b.hasRemaining() && completeLine(b)) {
                        ByteBuffer d = b.duplicate();
                        byte b1 = d.get();
                        if (LF == b1
                            || d.hasRemaining() && CR == b1 && LF == d.get()) {
                            b = null;
                            done = true;
                        } else {
                            state = FIRST_LINE_STATE;
                        }
                    } else if (b.remaining() > maxRequestLine) {
                        throw new ParseException("URI length exceeded: "
                                                 + AsciiCharBuffer.wrap(b));
                    } else {
                        if (b.capacity() < maxRequestLine) {
                            ByteBuffer r = ByteBuffer.allocate(maxRequestLine);
                            r.put(b);
                            b = r;
                        } else {
                            b.compact();
                        }
                        done = true;
                    }

                    break;
                }
            case FIRST_LINE_STATE:
                {
                    // Initialize the buffer, we'll need it until
                    // we're done with HEADER state.
                    this.buf = new byte[maxUri];

                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in FIRST_LINE_STATE");
                    }

                    if (completeLine(b)) {
                        tokenList.add(firstLine(b));

                        state = ACCUMULATE_HEADER_STATE;
                    } else {
                        b.compact();
                        done = true;
                    }
                    break;
                }
            case ACCUMULATE_HEADER_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in ACCUMULATE_HEADER_STATE");
                    }

                    if (!completeHeader(b)) {
                        if (b.capacity() < maxHeader) {
                            ByteBuffer nb = ByteBuffer.allocate(maxHeader + 2);
                            nb.put(b);
                            nb.flip();
                            b = nb;
                        } else if (b.remaining() >= maxHeader) {
                            String msg = "header exceeds " + maxHeader
                                + ":\n" + AsciiCharBuffer.wrap(b);
                            if (blockLongHeaders) {
                                logger.warn(msg);
                                // XXX send error page instead
                                session.shutdownClient();
                                session.shutdownServer();
                                return new ParseResult();
                            } else {
                                // allow session to be released, or not
                                throw new ParseException(msg);
                            }
                        }

                        b.compact();

                        done = true;
                    } else {
                        state = HEADER_STATE;
                    }
                    break;
                }
            case HEADER_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in HEADER_STATE");
                    }
                    header = header(b);
                    tokenList.add(header);

                    // Done with buf now
                    this.buf = null;

                    assert !b.hasRemaining();

                    if (!clientSide) {
                        if (null != requestLineToken) {
                            HttpMethod method = requestLineToken.getMethod();
                            if (HttpMethod.HEAD == method) {
                                transferEncoding = NO_BODY;
                            }
                        }
                    } else {
                        /* This is saved internally and used later with getRequestEvent */
                        HttpRequestEvent evt = new HttpRequestEvent(requestLineToken.getRequestLine(), header.getValue("host"), lengthCounter);
                    }

                    if (NO_BODY == transferEncoding) {
                        state = END_MARKER_STATE;
                    } else if (CLOSE_ENCODING == transferEncoding) {
                        lineBuffering(false);
                        b = null;
                        state = CLOSED_BODY_STATE;
                        done = true;
                    } else if (CHUNKED_ENCODING == transferEncoding) {
                        lineBuffering(true);
                        b = null;
                        state = CHUNK_LENGTH_STATE;
                        done = true;
                    } else if (CONTENT_LENGTH_ENCODING == transferEncoding) {
                        lineBuffering(false);
                        assert !b.hasRemaining();

                        if (0 < contentLength) {
                            readLimit(contentLength);
                            b = null;
                            state = CONTENT_LENGTH_BODY_STATE;
                            done = true;
                        } else {
                            state = END_MARKER_STATE;
                        }
                    } else {
                        assert false;
                    }
                    break;
                }
            case CLOSED_BODY_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in CLOSED_BODY_STATE!");
                    }
                    tokenList.add(closedBody(b));
                    b = null;
                    done = true;
                    break;
                }
            case CONTENT_LENGTH_BODY_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in CONTENT_LENGTH_BODY_STATE");
                    }
                    tokenList.add(chunk(b));
                    if (0 == contentLength) {
                        b = null;
                        // XXX handle trailer
                        state = END_MARKER_STATE;
                    } else {
                        readLimit(contentLength);
                        b = null;
                        done = true;
                    }
                    break;
                }
            case CHUNK_LENGTH_STATE:
                // chunk-size     = 1*HEX
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in CHUNK_LENGTH_STATE");
                    }
                    if (!completeLine(b)) {
                        b.compact();
                        done = true;
                        break;
                    }

                    contentLength = chunkLength(b);
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "CHUNK contentLength = "
                                     + contentLength);
                    }
                    if (0 == contentLength) {
                        b = null;
                        state = LAST_CHUNK_STATE;
                    } else {
                        lineBuffering(false);
                        assert !b.hasRemaining();

                        readLimit(contentLength);
                        b = null;

                        state = CHUNK_BODY_STATE;
                    }
                    done = true;
                    break;
                }
            case CHUNK_BODY_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in CHUNKED_BODY_STATE");
                    }

                    tokenList.add(chunk(b));

                    if (0 == contentLength) {
                        lineBuffering(true);
                        b = null;
                        state = CHUNK_END_STATE;
                    } else {
                        readLimit(contentLength);
                        b = null;
                    }

                    done = true;
                    break;
                }
            case CHUNK_END_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in END_CHUNK_STATE");
                    }

                    if (!completeLine(b)) {
                        b.compact();
                        done = true;
                        break;
                    }

                    eatCrLf(b);
                    assert !b.hasRemaining();

                    b = null;
                    done = true;

                    state = CHUNK_LENGTH_STATE;
                    break;
                }
            case LAST_CHUNK_STATE:
                // last-chunk     = 1*("0") [ chunk-extension ] CRLF
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in LAST_CHUNK_STATE");
                    }

                    if (!completeLine(b)) {
                        b.compact();
                        done = true;
                        break;
                    }

                    eatCrLf(b);

                    assert !b.hasRemaining();

                    b = null;

                    state = END_MARKER_STATE;
                    break;
                }
            case END_MARKER_STATE:
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug(sessStr + "in END_MARKER_STATE");
                    }
                    EndMarker endMarker = EndMarker.MARKER;
                    tokenList.add(endMarker);
                    lineBuffering(true);
                    b = null;
                    state = PRE_FIRST_LINE_STATE;

                    if (!clientSide) {
                        String contentType = header.getValue("content-type");
                        String mimeType = null == contentType ? null : MimeType.getType(contentType);

                        RequestLine rl = null == requestLineToken ? null : requestLineToken.getRequestLine();

                        if (null != rl) {
                            HttpResponseEvent evt = new HttpResponseEvent(rl, mimeType, lengthCounter);

                            casing.getNode().logEvent(evt);
                        }
                    } else {
                        HttpRequestEvent evt = requestLineToken.getRequestLine().getHttpRequestEvent();
                        evt.setContentLength(lengthCounter);

                        if (evt.getRequestUri() == null) {
                            logger.warn("null request for: " + getSession().sessionEvent());
                        }

                        casing.getNode().logEvent(evt);
                        
                        /**
                         * Update host table with header info
                         * if an entry already exists for this host
                         */
                        InetAddress clientAddr = getSession().sessionEvent().getCClientAddr();
                        String agentString = header.getValue("user-agent");
                        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr );
                        if (clientAddr != null && agentString != null && entry != null ) {
                            UserAgentString uas = new UserAgentString(agentString);
                            long setDate = entry.getHttpUserAgentSetDate();
                            
                            /**
                             * If the current agent string is null
                             * or if its not null it was set more than 60 seconds ago
                             * set the agent string and agent string information
                             */
                            if ( entry.getHttpUserAgent() == null || setDate == 0 ||
                                 ( System.currentTimeMillis() > setDate + (60*1000) ) ) {
                                entry.setHttpUserAgent( agentString );
                                logger.warn("Setting UAS OS : " + uas.getOsInfo() + " from " + agentString );
                                entry.setHttpUserAgentOs( uas.getOsInfo() );
                                entry.setHttpUserAgentSetDate( System.currentTimeMillis() );
                            }
                        }
                    }

                    // Free up header storage
                    header = null;
                    done = true;
                    break;
                }
            default:
                assert false;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + "returning readBuffer: " + b);
        }

        scheduleTimer(TIMEOUT);

        if (null != b && !b.hasRemaining()) {
            String msg = "b does not have remaining: " + b
                + " in state: " + state;
            b.flip();
            msg += " buffer contents: '" + AsciiCharBuffer.wrap(b) + "'";
            logger.error(msg);
            throw new ParseException(msg);
        }

        return new ParseResult(tokenList, b);
    }

    private boolean completeLine(ByteBuffer b)
    {
        return b.get(b.limit() - 1) == LF;
    }

    private boolean completeHeader(ByteBuffer b)
    {
        ByteBuffer d = b.duplicate();

        // no header
        if (d.remaining() > 0 && d.remaining() <= 2) {
            if (LF == d.get(d.limit() - 1)) {
                return true;
            }
        }

        if (d.remaining() >= 4) {
            d.position(d.limit() - 4);
        }

        byte c = ' ';
        while (CR != c && LF != c) {
            if (d.hasRemaining()) {
                c = d.get();
            } else {
                return false;
            }
        }

        if (LF == c || CR == c && d.hasRemaining() && LF == d.get()) {
            if (d.hasRemaining()) {
                c = d.get();
                return LF == c || CR == c && d.hasRemaining() && LF == d.get();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public ParseResult parseEnd(ByteBuffer b) throws ParseException
    {
        if (b.hasRemaining()) {
            switch (state) {
            case ACCUMULATE_HEADER_STATE:
                b.flip();
                List<Token> l = Collections.singletonList((Token)header(b));
                return new ParseResult(l, null);
            default:
                // I think we want to release in most circumstances
                throw new ParseException("in state: " + state
                                         + " data trapped in read buffer: "
                                         + b.remaining());
            }
        }

        // we should implement this to make sure end markers get sent always

        return new ParseResult();
    }

    public TokenStreamer endSession()
    {
        switch (state) {
        case PRE_FIRST_LINE_STATE:
            return null;

        case ACCUMULATE_HEADER_STATE:
            logger.warn("endSession in ACCUMULATE_HEADER_STATE");
            return null;

        case HEADER_STATE:
            logger.warn("endSession in HEADER_STATE");
            return null;

        case CONTENT_LENGTH_BODY_STATE:
            logger.warn("endSession in CONTENT_LENGTH_BODY_STATE, length: "
                        + contentLength);
            return endMarkerStreamer();

        case CHUNK_LENGTH_STATE:
            logger.warn("endSession in CHUNK_LENGTH_STATE");
            return endMarkerStreamer();

        case CHUNK_BODY_STATE:
            logger.warn("endSession in CHUNK_BODY_STATE, length: "
                        + contentLength);
            return endMarkerStreamer();

        case CHUNK_END_STATE:
            logger.warn("endSession in CHUNK_END_STATE");
            return endMarkerStreamer();

        case LAST_CHUNK_STATE:
            logger.warn("endSession in LAST_CHUNK_STATE");
            return endMarkerStreamer();

        case END_MARKER_STATE:
            logger.warn("endSession in END_MARKER_STATE");
            return endMarkerStreamer();

        case CLOSED_BODY_STATE:
            /* this case is legit */
            return endMarkerStreamer();

        default:
            logger.warn("endSession unhandled state: " + state);
            return null;
        }
    }

    public void handleTimer()
    {
        byte cs = session.clientState();
        byte ss = session.serverState();

        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + "handling timer cs=" + cs + " ss=" + ss);
        }

        if (cs == NodeTCPSession.HALF_OPEN_OUTPUT
            && ss == NodeTCPSession.HALF_OPEN_INPUT) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + "closing session in halfstate");
            }
            session.shutdownClient();
        } else {
            scheduleTimer(TIMEOUT);
        }
    }

    // private methods ---------------------------------------------------------

    private Token firstLine(ByteBuffer data) throws ParseException
    {
        if (!clientSide) {
            statusLine = statusLine(data);
            return statusLine;
        } else {
            return requestLineToken = requestLine(data);
        }
    }

    // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
    private RequestLineToken requestLine(ByteBuffer data) throws ParseException
    {
        transferEncoding = NO_BODY;

        HttpMethod method = HttpMethod.getInstance(token(data));
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + "method: " + method);
        }
        eat(data, SP);
        byte[] requestUri = requestUri(data);
        eat(data, SP);
        String httpVersion = version(data);
        eatCrLf(data);

        RequestLine rl = new RequestLine(getSession().sessionEvent(), method, requestUri);
        return new RequestLineToken(rl, httpVersion);
    }

    // Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
    private StatusLine statusLine(ByteBuffer data) throws ParseException
    {
        transferEncoding = CLOSE_ENCODING;

        String httpVersion = version(data);
        eat(data, SP);
        int statusCode = statusCode(data);
        eat(data, SP);
        String reasonPhrase = reasonPhrase(data);
        eatCrLf(data);

        // 4.4 Message Length
        // 1. Any response message which "MUST NOT" include a
        // message-body (such as the 1xx, 204, and 304 responses and
        // any response to a HEAD request) is always terminated by the
        // first empty line after the header fields, regardless of the
        // entity-header fields present in the message.
        if (100 <= statusCode && 199 >= statusCode
            || 204 == statusCode || 304 == statusCode) {
            transferEncoding = NO_BODY;
        }

        if (100 != statusCode && 408 != statusCode) {
            RequestLineToken rl = casing.dequeueRequest(statusCode);
            // casing returns null and logs an error when nothing in queue
            requestLineToken = null != rl ? rl : requestLineToken;
        }

        return new StatusLine(httpVersion, statusCode, reasonPhrase);
    }

    // HTTP-Version   = "HTTP" "/" 1*DIGIT "." 1*DIGIT
    private String version(ByteBuffer data) throws ParseException
    {
        eat(data, "HTTP");
        eat(data, '/');
        int maj = eatDigits(data);
        eat(data, '.');
        int min = eatDigits(data);

        return "HTTP/" + maj + "." + min;
    }

    // Reason-Phrase  = *<TEXT, excluding CR, LF>
    private String reasonPhrase(ByteBuffer b) throws ParseException
    {
        int l = b.remaining();

        for (int i = 0; b.hasRemaining(); i++) {
            if (isCtl(buf[i] = b.get())) {
                b.position(b.position() - 1);
                return new String(buf, 0, i);
            }
        }

        return new String(buf, 0, l);
    }

    // Status-Code    =
    //       "100"  ; Section 10.1.1: Continue
    //     | ...
    //     | extension-code
    // extension-code = 3DIGIT
    private int statusCode(ByteBuffer b) throws ParseException
    {
        int i = eatDigits(b);

        if (1000 < i || 100 > i) {
            // assumes no status codes begin with 0
            throw new ParseException("expected 3 DIGITs, got: " + i);
        }

        return i;
    }

    private Header header(ByteBuffer data) throws ParseException
    {
        Header header = new Header();

        while (data.remaining() > 2) {
            field(header, data);
            eatCrLf(data);
        }

        while (data.hasRemaining()) {
            eatCrLf(data);
        }

        return header;
    }

    // message-header = field-name ":" [ field-value ]
    // field-name     = token
    // field-value    = *( field-content | LWS )
    // field-content  = <the OCTETs making up the field-value
    //                  and consisting of either *TEXT or combinations
    //                  of token, separators, and quoted-string>
    private void field(Header header, ByteBuffer data)
        throws ParseException
    {
        String key = token(data).trim();
        eat(data, ':');
        String value = eatText(data).trim();

        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + "field key: " + key + " value: " + value);
        }

        // 4.3: The presence of a message-body in a request is signaled by the
        // inclusion of a Content-Length or Transfer-Encoding header field in
        // the request's message-headers.
        // XXX check for valid body in the *reply* as well!
        if (key.equalsIgnoreCase("transfer-encoding")) {
            if (value.equalsIgnoreCase("chunked")) {
                if (logger.isDebugEnabled()) {
                    logger.debug(sessStr + "using chunked encoding");
                }
                transferEncoding = CHUNKED_ENCODING;
            } else {
                logger.warn("don't know transfer-encoding: " + value);
            }
        } else if (key.equalsIgnoreCase("content-length")
                   && transferEncoding != CHUNKED_ENCODING) {

            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + "using content length encoding");
            }
            transferEncoding = CONTENT_LENGTH_ENCODING;
            contentLength = Integer.parseInt(value);
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + "CL contentLength = " + contentLength);
            }
        } else if (key.equalsIgnoreCase("accept-encoding")) {
            //value = "identity";
        }

        header.addField(key, value);
    }

    private Chunk closedBody(ByteBuffer buffer) throws ParseException
    {
        lengthCounter += buffer.remaining();
        return new Chunk(buffer.slice());
    }

    private int chunkLength(ByteBuffer b) throws ParseException
    {
        int i = 0;

        while (b.hasRemaining()) {
            byte c = b.get();
            if (isHex(c)) {
                i = 16 * i + hexValue((char)c);
            } else if (';' == c) {
                // XXX
                logger.warn(sessStr + "chunk extension not supported yet");
            } else if (CR == c || LF == c) {
                b.position(b.position() - 1);
                break;
            } else if (SP == c) {
                // ignore spaces
            } else {
                // XXX
                logger.warn(sessStr + "unknown character in chunk length: " + c);
            }
        }

        eatCrLf(b);

        return i;
    }

    // chunk          = chunk-size [ chunk-extension ] CRLF
    //                  chunk-data CRLF
    private Chunk chunk(ByteBuffer buffer) throws ParseException
    {
        int remaining = buffer.remaining();
        contentLength -= remaining;
        lengthCounter += remaining;

        assert 0 <= contentLength;

        return new Chunk(buffer.slice());
    }

    // Request-URI    = "*" | absoluteURI | abs_path | authority
    private byte[] requestUri(ByteBuffer b)
        throws ParseException
    {
        ByteBuffer dup = b.duplicate();

        for (int i = 0; b.hasRemaining(); i++) {
            if (maxUri <= i && blockLongUris) {
                String msg = "(buf limit exceeded) " + buf.length
                    + ": " + new String(buf);
                session.shutdownClient();
                session.shutdownServer();
                throw new ParseException("blocking " + msg);
            }

            char c = (char)b.get();

            if (SP == c || HT == c) {
                b.position(b.position() - 1);
                dup.limit(b.position());
                break;
            }
        }

        byte[] a = new byte[dup.remaining()];
        dup.get(a);

        return a;
    }

    private void eat(ByteBuffer data, String s) throws ParseException
    {
        byte[] sb = s.getBytes();
        for (int i = 0; i < sb.length; i++) {
            eat(data, sb[i]);
        }
    }

    private boolean eat(ByteBuffer data, char c)
    {
        return eat(data, (byte)c);
    }

    private boolean eat(ByteBuffer data, byte c)
    {
        if (!data.hasRemaining()) {
            return false;
        }

        int b = data.get();
        if (b != c) {
            logger.debug(sessStr + "expected " + b + " bytes, but got " + c + " bytes.");
            data.position(data.position() - 1);
            return false;
        } else {
            return true;
        }
    }

    // read *TEXT, folding LWS
    // TEXT           = <any OCTET except CTLs,
    //                  but including LWS>
    private String eatText(ByteBuffer b) throws ParseException
    {
        eatLws(b);

        int l = b.remaining();

        for (int i = 0; b.hasRemaining(); i++) {
            if (buf.length <= i) {
                String msg = "(buf limit exceeded) " + buf.length
                    + ": " + new String(buf);
                if (blockLongUris) {
                    session.shutdownClient();
                    session.shutdownServer();
                    throw new ParseException("blocking " + msg);
                } else {
                    throw new ParseException("non-http " + msg);
                }
            }
            buf[i] = b.get();
            if (isCtl(buf[i])) {
                b.position(b.position() - 1);
                if (eatLws(b)) {
                    buf[i] = SP;
                } else {
                    byte b1 = b.get(b.position());
                    byte b2 = b.get(b.position() + 1);
                    if (LF == b1 || CR == b1 && LF == b2) {
                        return new String(buf, 0, i);
                    } else {
                        b.get();
                        // XXX make this configurable
                        // microsoft IIS thinks its ok to put CTLs in headers
                    }
                }
            }
        }

        return new String(buf, 0, l);
    }

    // LWS            = [CRLF] 1*( SP | HT )
    private boolean eatLws(ByteBuffer b)
    {
        int s = b.position();

        byte b1 = b.get();
        if (CR == b1 && b.hasRemaining()) {
            if (LF != b.get()) {
                b.position(b.position() - 2);
                return false;
            }
        } else if (LF != b1) {
            b.position(b.position() - 1);
        }

        boolean result = false;
        while (b.hasRemaining()) {
            byte c = b.get();
            if (SP != c && HT != c) {
                b.position(b.position() - 1);
                break;
            } else {
                result = true;
            }
        }

        if (!result) {
            b.position(s);
        }

        return result;
    }

    // CRLF           = CR LF
    // in our implementation, CR is optional
    private void eatCrLf(ByteBuffer b) throws ParseException
    {
        byte b1 = b.get();
        boolean ate = LF == b1 || CR == b1 && LF == b.get();
        if (!ate) {
            throw new ParseException("CRLF expected: " + b1);
        }
    }

    // DIGIT          = <any US-ASCII digit "0".."9">
    // this method reads 1*DIGIT
    private int eatDigits(ByteBuffer b) throws ParseException
    {
        boolean foundOne = false;
        int i = 0;

        while (b.hasRemaining()) {
            if (isDigit(b.get(b.position()))) {
                foundOne = true;
                i = i * 10 + (b.get() - '0');
            } else {
                break;
            }
        }

        if (!foundOne) {
            throw new ParseException("no digits found");
        }

        return i;
    }

    // token          = 1*<any CHAR except CTLs or separators>
    private String token(ByteBuffer b)
    {
        int l = b.remaining();

        for (int i = 0; b.hasRemaining(); i++) {
            buf[i] = b.get();
            if (isCtl(buf[i]) || isSeparator(buf[i])) {
                b.position(b.position() - 1);
                return new String(buf, 0, i);
            }
        }

        return new String(buf, 0, l);
    }

    // separators     = "(" | ")" | "<" | ">" | "@"
    //                | "," | ";" | ":" | "\" | <">
    //                | "/" | "[" | "]" | "?" | "="
    //                | "{" | "}" | SP | HT
    private boolean isSeparator(byte b)
    {
        switch (b) {
        case '(': case ')': case '<': case '>': case '@':
        case ',': case ';': case ':': case '\\': case '"':
        case '/': case '[': case ']': case '?': case '=':
        case '{': case '}': case SP: case HT:
            return true;
        default:
            return false;
        }
    }

    // DIGIT          = <any US-ASCII digit "0".."9">
    private boolean isDigit(byte b)
    {
        return '0' <= b && '9' >= b;
    }

    private boolean isHex(byte b)
    {
        switch (b) {
        case '0': case '1': case '2': case '3': case '4': case '5':
        case '6': case '7': case '8': case '9': case 'a': case 'b':
        case 'c': case 'd': case 'e': case 'f': case 'A': case 'B':
        case 'C': case 'D': case 'E': case 'F':
            return true;
        default:
            return false;
        }
    }

    private int hexValue(char c)
    {
        switch (c) {
        case '0': return 0;
        case '1': return 1;
        case '2': return 2;
        case '3': return 3;
        case '4': return 4;
        case '5': return 5;
        case '6': return 6;
        case '7': return 7;
        case '8': return 8;
        case '9': return 9;
        case 'A': case 'a': return 10;
        case 'B': case 'b': return 11;
        case 'C': case 'c': return 12;
        case 'D': case 'd': return 13;
        case 'E': case 'e': return 14;
        case 'F': case 'f': return 15;
        default: throw new IllegalArgumentException("expects hex digit");
        }
    }

    // CTL            = <any US-ASCII control character
    //                  (octets 0 - 31) and DEL (127)>
    boolean isCtl(byte b)
    {
        return 0 <= b && 31 >= b || 127 == b;
    }

    boolean isUpAlpha(byte b)
    {
        return 'A' <= b && 'Z' >= b;
    }

    boolean isLoAlpha(byte b)
    {
        return 'a' <= b && 'z' >= b;
    }

    boolean isAlpha(byte b)
    {
        return isUpAlpha(b) || isLoAlpha(b);
    }

    private TokenStreamer endMarkerStreamer()
    {
        return new TokenStreamer()
            {
                private boolean sent = false;

                public boolean closeWhenDone() { return true; }

                public Token nextToken()
                {
                    if (sent) {
                        return null;
                    } else {
                        sent = true;
                        return EndMarker.MARKER;
                    }
                }
            };
    }
}
