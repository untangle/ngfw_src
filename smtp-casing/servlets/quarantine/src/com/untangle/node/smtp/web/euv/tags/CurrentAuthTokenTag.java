/**
 * $Id: CurrentAuthTokenTag.java 35079 2013-06-19 22:15:28Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import java.net.URLEncoder;
import javax.servlet.ServletRequest;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

/**
 * Outputs the current auth token (URL encoded optionaly), or null
 * if there 'aint one
 */
@SuppressWarnings("serial")
public final class CurrentAuthTokenTag extends SingleValueTag 
{
    private final Logger logger = Logger.getLogger(CurrentAuthTokenTag.class);
    
    private static final String AUTH_TOKEN_KEY = "untangle.auth_token";
    private static final String EL_AUTH_TOKEN_KEY = "currentAuthToken";

    private boolean m_encoded = false;

    public void setEncoded(boolean encoded)
    {
        m_encoded = encoded;
    }

    public boolean isEncoded()
    {
        return m_encoded;
    }

    @Override
    protected String getValue()
    {
        String s = null;
        if(hasCurrent(pageContext.getRequest())) {
            s = getCurrent(pageContext.getRequest());
            if(isEncoded()) {
                try {
                    s = URLEncoder.encode(s,"UTF-8");
                } catch (UnsupportedEncodingException e) {    
                    logger.warn("Unsupported Encoding:",e);
                    s = "";
                }
            }    
        }
        return s;
    }

    public static final void setCurrent(ServletRequest request, String token)
    {
        request.setAttribute(AUTH_TOKEN_KEY, token);
        request.setAttribute(EL_AUTH_TOKEN_KEY, token );
    }

    public static final void clearCurret(ServletRequest request)
    {
        request.removeAttribute(AUTH_TOKEN_KEY);
        request.removeAttribute(EL_AUTH_TOKEN_KEY);
    }

    /**
     * Returns null if there is no current token
     */
    static String getCurrent(ServletRequest request)
    {
        return (String) request.getAttribute(AUTH_TOKEN_KEY);
    }

    static boolean hasCurrent(ServletRequest request)
    {
        return getCurrent(request) != null;
    }
}
