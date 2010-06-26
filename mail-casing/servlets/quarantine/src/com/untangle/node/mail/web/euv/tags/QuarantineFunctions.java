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
package com.untangle.node.mail.web.euv.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;

import org.apache.log4j.Logger;
import org.json.JSONArray;

public class QuarantineFunctions
{
    private static final Logger logger = Logger.getLogger(QuarantineFunctions.class);
    
    private static final String SL_KEY = "untangle.safelist.contents.";
    private static final String INBOX_CURSOR_KEY = "untangle.inbox_cursor";
    private static final String REMAPS_KEY = "untangle.remapps.ReceivingRemapsListTag";
    private static final String MESSAGES_KEY_PREFIX = "untangle.messages.";
    private static final String ERROR_MSG_SUFFIX = "error";
    private static final String INFO_MSG_SUFFIX = "info";

    public static String remappedTo( PageContext pageContext, boolean isEncoded )
    {
        String remapped = RemappedToTag.getCurrent(pageContext.getRequest());
        if ( remapped == null ) return null;

        if ( isEncoded ) {
        	try {
        		remapped = URLEncoder.encode( remapped , "UTF-8");
        	} catch (UnsupportedEncodingException e) {	
            	logger.warn("Unsupported Encoding:",e);
            }	
        }
            
        return remapped;
    }

    public static String maxDaysToIntern( PageContext pageContext )
    {
        return MaxDaysToInternTag.getMaxDays( pageContext.getRequest());
    }

    public static int totalMessageCount( PageContext pageContext )
    {
        InboxRecordCursor cursor = getCurrentIndex(pageContext.getRequest());
        if ( cursor == null ) return 0;
        return (int)cursor.inboxCount();
    }
    
    public static String jsonSafelist( PageContext pageContext )
    {
        return buildJsonList( getCurrentSafelist(pageContext.getRequest()));
    }

    /* This is a list of addresses that are redirected to this user account */
    public static String jsonReceivingRemaps( PageContext pageContext )
    {
        return buildJsonList( getCurrentRemapsList(pageContext.getRequest()));
    }

    private static final String buildJsonList( String[] values )
    {
        if ( values == null || values.length == 0 ) {
            return "[]";
        }

        JSONArray ja = new JSONArray();
        for ( String value : values ) {
            JSONArray v = new JSONArray();
            v.put( value );
            ja.put( v );
        }
        
        return ja.toString();
    }

    public static void setCurrentSafelist( ServletRequest request, String[] list )
    {
        request.setAttribute(SL_KEY, list);
    }

    public static void setCurrentRemapsList( ServletRequest request, String[] list )
    {
        request.setAttribute(REMAPS_KEY, list);
    }

    public static final void setCurrentIndex(ServletRequest request, InboxRecordCursor index) 
    {
        request.setAttribute(INBOX_CURSOR_KEY, index);
    }
    
    public static final void clearCurrentIndex( ServletRequest request )
    {
        request.removeAttribute(INBOX_CURSOR_KEY);
    }

    public static final void setErrorMessages(ServletRequest request, String...messages )
    {
        setMessages(request, ERROR_MSG_SUFFIX, messages);
    }

    public static final void setInfoMessages( ServletRequest request, String...messages )
    {
        setMessages(request, INFO_MSG_SUFFIX, messages);
    }

    public static final void setMessages(ServletRequest request, String msgType, String...messages) 
    {
        for( String msg : messages ) addMessage( request, msgType, msg );
    }

    public static final void addErrorMessage( ServletRequest request, String message )
    {
        addMessage(request, ERROR_MSG_SUFFIX, message);
    }

    public static final void addInfoMessage( ServletRequest request, String message )
    {
        addMessage( request, INFO_MSG_SUFFIX, message );
    }

    public static final void addMessage( ServletRequest request, String msgType, String msg ) 
    {
        ArrayList<String> list = getMessages(request, msgType);
        if(list == null) {
            list = new ArrayList<String>();
            request.setAttribute(MESSAGES_KEY_PREFIX + msgType, list);
        }
        list.add(msg);
    }

    public static final void clearMessages( ServletRequest request, String msgType ) 
    {
        request.removeAttribute(MESSAGES_KEY_PREFIX + msgType);
    }

    /**
     * Returns null if there are no safelist entries
     * - sort string entries, within list, according to natural, ascending order
     */
    static String[] getCurrentSafelist(ServletRequest request) {
        Object allSLEntries = request.getAttribute(SL_KEY);
        if (null != allSLEntries) {
            Arrays.sort((String[]) allSLEntries);
        }
        return (String[]) allSLEntries;
    }

    static boolean hasCurrentLSafelst(ServletRequest request) {
        String[] list = getCurrentSafelist(request);
        return list != null && list.length > 0;
    }

    /**
     * Returns null if there is no index
     */
    static InboxRecordCursor getCurrentIndex(ServletRequest request) {
        return (InboxRecordCursor) request.getAttribute(INBOX_CURSOR_KEY);
    }

    static boolean hasCurrentIndex(ServletRequest request) {
        InboxRecordCursor index = getCurrentIndex(request);
        return index != null && index.size() > 0;
    }

    /**
     * Returns null if there are no such messages
     */
    static String[] getCurrentRemapsList(ServletRequest request)
    {
        return (String[]) request.getAttribute(REMAPS_KEY);
    }

    static boolean hasCurrentRemapsList(ServletRequest request)
    {
        String[] list = getCurrentRemapsList(request);
        return list != null && list.length > 0;
    }

    /**
     * Returns null if there are no such messages
     */
    @SuppressWarnings("unchecked") //getAttribute
    private static ArrayList<String> getMessages(ServletRequest request, String msgType) 
    {
        return (ArrayList<String>) request.getAttribute(MESSAGES_KEY_PREFIX + msgType);
    }

    static boolean hasMessages(ServletRequest request,
                               String msgType) {
        ArrayList<String> msgs = getMessages(request, msgType);
        return msgs != null && msgs.size() > 0;
    }
}
