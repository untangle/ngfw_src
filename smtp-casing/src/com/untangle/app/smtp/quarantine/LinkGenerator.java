/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.net.URLEncoder;

import org.apache.log4j.Logger;

/**
 * Little class used to generate links in digest emails. It exists to be "called" from a Velocity template. <br>
 * <br>
 * Instance is stateful (it "knows" the current address being templated).
 */
public class LinkGenerator
{
    private String m_urlBase;

    private final Logger logger = Logger.getLogger(LinkGenerator.class);
    private static final String AUTH_TOKEN_RP = "tkn";
    
    /**
     * Initialize instance of LinkGenerator.
     * @param base Base.
     * @param authTkn String of auth token.
     */
    LinkGenerator(String base, String authTkn) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://");
        sb.append(base);
        sb.append("/quarantine/manageuser?");
        sb.append(AUTH_TOKEN_RP);
        sb.append('=');
        try {
            sb.append(URLEncoder.encode(authTkn, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding:", e);
        }

        m_urlBase = sb.toString();
    }

    /**
     * Return inbox link.
     * @return String of inbox link.
     */
    public String generateInboxLink()
    {
        return m_urlBase;
    }

    /**
     * Return help link.
     * @return String of helpbox link.
     */
    public String generateHelpLink()
    {
        return "help_link";
    }

}