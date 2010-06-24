/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.quarantine;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.quarantine.WebConstants;

/**
 * Little class used to generate links in digest emails.
 * It exists to be "called" from a Velocity template.
 * <br><br>
 * Instance is stateful (it "knows" the current address
 * being templated).
 */
public class LinkGenerator {

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
