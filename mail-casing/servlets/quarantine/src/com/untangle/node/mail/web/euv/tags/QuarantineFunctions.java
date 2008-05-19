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

import java.net.URLEncoder;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import com.untangle.node.mail.web.euv.Constants;

import com.untangle.node.mail.papi.quarantine.InboxRecordCursor;

public class QuarantineFunctions
{
    public static String remappedTo( PageContext pageContext, boolean isEncoded )
    {
        String remapped = RemappedToTag.getCurrent(pageContext.getRequest());
        if ( remapped == null ) return null;

        if ( isEncoded ) remapped = URLEncoder.encode( remapped );
            
        return remapped;
    }

    public static String maxDaysToIntern( PageContext pageContext )
    {
        return MaxDaysToInternTag.getMaxDays( pageContext.getRequest());
    }

    public static int totalMessageCount( PageContext pageContext )
    {
        InboxRecordCursor cursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
        if ( cursor == null ) return 0;
        return (int)cursor.inboxCount();
    }
    
    public static String url( PageContext pageContext, String type )
    {
        String query = "";
        if ( "map_view".equals( type )) {
            return buildQuery( pageContext, Constants.MAP_CTL, Constants.MAPVIEW_VIEW_RV );
        } else if ( "unmap_view".equals( type )) {
            return buildQuery( pageContext, Constants.UNMAP_CTL, Constants.UNMAPPER_VIEW_RV );
        } else if ( "safelist_view".equals( type )) {
            return buildQuery( pageContext,Constants.SAFELIST_CTL, Constants.SAFELIST_VIEW_RV );
        } else {
            System.err.println( "Unable to handle the type: " + type );
            return "";
        }
    }

    public static String jsonSafelist( PageContext pageContext )
    {
        return buildJsonList( SafelistListTag.getCurrentList(pageContext.getRequest()));
    }

    /* This is a list of addresses that are redirected to this user account */
    public static String jsonReceivingRemaps( PageContext pageContext )
    {
        return buildJsonList( ReceivingRemapsListTag.getCurrentList(pageContext.getRequest()));
    }

    public static String buildQuery( PageContext pageContext, String controller, String action )
    {
        String query = "/quarantine" + controller + "?";
        query += Constants.ACTION_RP + "=" + URLEncoder.encode( action );

        
        String token = CurrentAuthTokenTag.getCurrent( pageContext.getRequest());
        if ( token != null ) query += "&" + Constants.AUTH_TOKEN_RP + "=" + URLEncoder.encode( token );

        return query;
    }

    private static final String buildJsonList( String[] values )
    {
        if ( values == null ) return "[]";
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        sb.append( "[" );
        for ( String s : values ) {
            if ( !isFirst ) sb.append( "," );
            isFirst = false;
            sb.append( "['" + s + "']\n" );
        }

        sb.append( "]" );

        return  sb.toString();
    }
}
