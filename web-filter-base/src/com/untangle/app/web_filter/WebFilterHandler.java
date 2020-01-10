/**
 * $Id: WebFilterHandler.java 42622 2016-03-08 23:00:30Z dmorris $
 */

package com.untangle.app.web_filter;

import java.net.URI;

import com.untangle.app.http.HeaderToken;
import com.untangle.app.http.HttpRedirect;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.app.web_filter.WebFilterHandler;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Blocks HTTP traffic that is on an active block list.
 */
public class WebFilterHandler extends WebFilterBaseHandler
{

    /**
     * Constructor
     * 
     * @param app
     *        The web filter base application
     */
    public WebFilterHandler(WebFilterBase app)
    {
        super(app);
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
        app.incrementScanCount();

        HttpRedirect redirect = app.getDecisionEngine().checkRequest(session, session.getClientAddr(), session.getServerPort(), getRequestLine(session), requestHeader);
        // instead of details, get Token[] response
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + redirect);
        }

        if (redirect == null) {
            String host = requestHeader.getValue("Host");
            URI uri = getRequestLine(session).getRequestUri();

            if (app.getSettings().getEnforceSafeSearch()) {
                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + uri + "'");

                URI safeSearchUri = UrlRewriter.getSafeSearchUri(host, uri);

                if (safeSearchUri != null) getRequestLine(session).setRequestUri(safeSearchUri);

                logger.debug("doRequestHeader: host = '" + host + "', uri = '" + getRequestLine(session).getRequestUri() + "'");
            }

            releaseRequest(session);
        } else {
            String uri = getRequestLine(session).getRequestUri().toString();

            if(redirect.getType() == HttpRedirect.RedirectType.BLOCK){
                app.incrementBlockCount();
                blockRequest(session, redirect.getResponse());
            }else{
                app.incrementRedirectCount();
                redirectRequest(session, redirect.getResponse());
            }
        }

        return requestHeader;
    }
}
