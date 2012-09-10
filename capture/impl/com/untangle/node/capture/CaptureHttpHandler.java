/*
 * $Id: CaptureHttpHandler.java 31921 2012-05-12 02:44:47Z mahotz $
 */

package com.untangle.node.capture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
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
import com.untangle.node.util.TempFileFactory;
import com.untangle.node.util.GlobUtil;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.vnet.NodeTCPSession;

class CaptureHttpHandler extends HttpStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptureNodeImpl node;

    // constructors -----------------------------------------------------------

    CaptureHttpHandler(NodeTCPSession session, CaptureNodeImpl node)
    {
        super(session);
        this.node = node;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        NodeTCPSession sess = getSession();
        String method = getRequestLine().getMethod().toString();
        String host = getRequestLine().getRequestUri().getHost();
        String uri = getRequestLine().getRequestUri().toString();
               

        if (host == null)
        {
            host = requestHeader.getValue("host");
        }

        if (host == null)
        {
            InetAddress clientIp = getSession().getClientAddr();
            host = clientIp.getHostAddress();
        }

        host = host.toLowerCase();

        CaptureBlockDetails details = new CaptureBlockDetails(host, uri, method);
        logger.debug("doRequestHeader " + details.toString());
        Token[] response = node.generateResponse(details, sess);
        blockRequest(response);
        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk) throws TokenException
    {
        logger.debug("doRequestBody");
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() throws TokenException
    {
        logger.debug("doRequestBodyEnd");
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        logger.debug("doResponseHeader");
        return header;
    }

    @Override
    protected Chunk doResponseBody( Chunk chunk ) throws TokenException
    {
        logger.debug("doResponseBody");
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd( ) throws TokenException
    {
        logger.debug("doResponseBodyEnd");
    }

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine) throws TokenException
    {
        logger.debug("doRequestLine");
        return requestLine;
    }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine) throws TokenException
    {
        logger.debug("doStatusLine");
        return statusLine;
    }

    // private methods --------------------------------------------------------
}
