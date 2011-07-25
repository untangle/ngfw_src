/*
 * $Id$
 */
package com.untangle.node.phish;

import java.net.InetAddress;
import java.net.URI;
import org.apache.log4j.Logger;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.node.util.GoogleSafeBrowsingHashSet;

public class PhishHttpHandler extends HttpStateMachine
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String GOOGLE_HASH_DB_FILE  = "/usr/share/untangle-google-safebrowsing/lib/goog-black-hash";
    
    private final PhishNode node;

    private static GoogleSafeBrowsingHashSet googlePhishHashList = null;
    
    // constructors -----------------------------------------------------------

    PhishHttpHandler(TCPSession session, PhishNode node)
    {
        super(session);

        this.node = node;

        synchronized(this) {
            if (googlePhishHashList == null) 
                googlePhishHashList = new GoogleSafeBrowsingHashSet(GOOGLE_HASH_DB_FILE);
        }
        
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        //node.incrementScanCount();
        
        RequestLineToken rlToken = getRequestLine();
        URI uri = rlToken.getRequestUri();
        boolean isBlocked = false;

        String host = uri.getHost();
        if (host == null) {
            host = requestHeader.getValue("host");
            if (host == null) {
                InetAddress clientIp = getSession().clientAddr();
                host = clientIp.getHostAddress();
            }
        }
        host = host.toLowerCase();

        if (!node.getPhishSettings().getEnableGooglePhishList())
            isBlocked = false;
        else if (node.isDomainUnblocked(host, getSession().clientAddr())) 
            isBlocked = false;
        else if( googlePhishHashList.contains(host, uri.toString()) ) 
            isBlocked = true;

        if (isBlocked) {
            node.incrementBlockCount();
                
            // XXX change this category value
            node.logHttp(new PhishHttpEvent(rlToken.getRequestLine(), Action.BLOCK, "Google Safe Browsing"));

            InetAddress clientIp = getSession().clientAddr();

            PhishBlockDetails bd = new PhishBlockDetails
                (host, uri.toString(), clientIp);

            Token[] r = node.generateResponse(bd, getSession(),
                                              isRequestPersistent());

            blockRequest(r);
            return requestHeader;
        }
        
        node.incrementPassCount();
        
        releaseRequest();
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
        releaseResponse();
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        return header;
    }

    @Override
    protected Chunk doResponseBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd()
    {
    }
}
