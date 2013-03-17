/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.quarantine.WebConstants;

/**
 * Little class used to generate links in digest emails.
 * It exists to be "called" from a Velocity template.
 * <br><br>
 * Instance is stateful (it "knows" the current address
 * being templated).
 */
public class LinkGenerator
{
    private String m_urlBase;
    
    private final Logger logger = Logger.getLogger(LinkGenerator.class);

    LinkGenerator(String base, String authTkn) 
    {
        StringBuilder sb = new StringBuilder();
        sb.append("https://");
        sb.append(base);
        sb.append("/quarantine/manageuser?");
        sb.append(WebConstants.AUTH_TOKEN_RP);
        sb.append('=');
        try { 
        	sb.append(URLEncoder.encode(authTkn,"UTF-8"));
        } catch(java.io.UnsupportedEncodingException e) {
        	logger.warn("Unsupported Encoding:",e);
        }

        m_urlBase = sb.toString();
    }


    public String generateInboxLink() {
        return appendNVP(m_urlBase,
                         WebConstants.ACTION_RP,
                         WebConstants.VIEW_INBOX_RV);
    }
    public String generateHelpLink() {
        return "help_link";
    }

    /**
     * Generate a link to rescue the given mail
     *
     * @param mid the mail ID
     *
     * @return the complete URL ("http://... etc");
     */
    public String generateRescueLink(String mid) {
        return appendNVP(appendNVP(m_urlBase,
                                   WebConstants.ACTION_RP,
                                   WebConstants.RESCUE_RV),
                         WebConstants.MAIL_ID_RP, mid);
    }

    /**
     * Generate a link to purge the given mail
     *
     * @param mid the mail ID
     *
     * @return the complete URL ("http://... etc");
     */
    public String generatePurgeLink(String mid) {
        return appendNVP(appendNVP(m_urlBase,
                                   WebConstants.ACTION_RP,
                                   WebConstants.PURGE_RV),
                         WebConstants.MAIL_ID_RP, mid);
    }

    private String appendNVP(String base, String name, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        sb.append('&').append(name).append('=');
        try {
        	sb.append(URLEncoder.encode(value,"UTF-8"));
        } catch (UnsupportedEncodingException e) {	
        	logger.warn("Unsupported Encoding:",e);
        }
        return sb.toString();
    }

}
