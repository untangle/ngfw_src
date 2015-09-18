/**
 * $Id$
 */
package com.untangle.node.web_filter;

import java.net.URI;

import com.untangle.node.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.node.web_filter.WebFilterBase;
import com.untangle.node.web_filter.WebFilterHandler;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Blocks HTTP traffic that is on an active block list.
 */
public class WebFilterHandler extends WebFilterBaseHandler
{
    // constructors -----------------------------------------------------------

    WebFilterHandler( WebFilterBase node )
    {
        super( node );
    }

    @Override
    protected HeaderToken doRequestHeader( NodeTCPSession session, HeaderToken requestHeader )
    {
        node.incrementScanCount();

        String nonce = node.getDecisionEngine().checkRequest( session, session.getClientAddr(), 80, getRequestLine( session ), requestHeader );
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + nonce);
        }

        if (nonce == null) {
            String host = requestHeader.getValue("Host");
            URI uri = getRequestLine( session ).getRequestUri();

            if (node.getSettings().getEnforceSafeSearch()) {
                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + uri + "'");

                URI safeSearchUri = UrlRewriter.getSafeSearchUri(host, uri);

                if (safeSearchUri != null)
                    getRequestLine( session ).setRequestUri(safeSearchUri);

                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + getRequestLine( session ).getRequestUri() + "'");
            }

            if (node.getSettings().getEnforceYoutubeForSchools()) {
                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + uri + "'");

                URI youtubeForSchoolsUri = UrlRewriter.getYoutubeForSchoolsUri(host, uri, node.getSettings().getYoutubeForSchoolsIdentifier());

                if (youtubeForSchoolsUri != null)
                    getRequestLine( session ).setRequestUri(youtubeForSchoolsUri);

                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + getRequestLine( session ).getRequestUri() + "'");
            }

            releaseRequest( session );
        } else {
            node.incrementBlockCount();
            String uri = getRequestLine( session ).getRequestUri().toString();
            Token[] response = node.generateResponse( nonce, session, uri, requestHeader );

            blockRequest( session, response );
        }

        return requestHeader;
    }
}
