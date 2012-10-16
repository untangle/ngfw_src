/*
 * $Id: CaptureHttpHandler.java 31921 2012-05-12 02:44:47Z mahotz $
 */

package com.untangle.node.capture; // IMPL

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
        String clientAddr = getSession().getClientAddr().getHostAddress().toString();
        String serverAddr = getSession().getServerAddr().getHostAddress().toString();

        // see if the client is authenticated
        CaptureUserEntry user = node.captureUserTable.searchByAddress(clientAddr);

            // found in the table so update activity and allow traffic
            if (user != null)
            {
                user.updateActivityTimer();
                node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
                releaseRequest();
                return(requestHeader);
            }

            // check all the rules to see if traffic is allowed
            if (node.isSessionAllowed(clientAddr,serverAddr) == true)
            {
                // TODO event log here
                node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
                releaseRequest();
                return(requestHeader);
            }

        logger.info("Sending HTTP redirect to unauthenticated user " + clientAddr);

        String method = getRequestLine().getMethod().toString();
        String uri = getRequestLine().getRequestUri().toString();

        // look for a host in the request line
        String host = getRequestLine().getRequestUri().getHost();

        // if not found there look in the request header
        if (host == null) host = requestHeader.getValue("host");

        // if still not found then just use the IP address of the server
        if (host == null) host = getSession().getServerAddr().getHostAddress().toString();

        host = host.toLowerCase();

        CaptureBlockDetails details = new CaptureBlockDetails(host, uri, method);
        Token[] response = node.generateResponse(details, sess);
        node.incrementBlinger(CaptureNode.BlingerType.SESSBLOCK,1);
        blockRequest(response);
        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk) throws TokenException
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() throws TokenException
    {
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        releaseResponse();
        return header;
    }

    @Override
    protected Chunk doResponseBody( Chunk chunk ) throws TokenException
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd( ) throws TokenException
    {
    }

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine) throws TokenException
    {
        return requestLine;
    }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine) throws TokenException
    {
        return statusLine;
    }
}
