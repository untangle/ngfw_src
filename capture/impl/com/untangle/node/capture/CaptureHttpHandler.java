/*
 * $Id: CaptureHttpHandler.java 31921 2012-05-12 02:44:47Z mahotz $
 */

package com.untangle.node.capture;

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
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        logger.debug("doRequestLine");
        releaseRequest();
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        logger.debug("doRequestHeader");
        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk)
    {
        logger.debug("doRequestBody");
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd()
    {
        logger.debug("doRequestBodyEnd");
    }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        logger.debug("doStatusLine");
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        logger.debug("doResponseHeader");
        return header;
    }

    @Override
    protected Chunk doResponseBody(Chunk chunk) throws TokenException
    {
        logger.debug("doResponseBody");
        releaseResponse();
        return(chunk);
    }

    @Override
    protected void doResponseBodyEnd()
    {
        logger.debug("doResponseBodyEnd()");
    }

    // private methods --------------------------------------------------------
}
