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

package com.untangle.uvm.engine;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.security.UvmPrincipal;

class SpecialSingleSignOn extends SingleSignOn
{
    /* This is a set of all of the context paths that use UvmPrincipal */
    /* Dirty hack designed to ignore sessions that are in this context path */
    private final Set<String> uvmContextSet;

    private final UvmContext uvmContext;

    private final Logger logger = Logger.getLogger(getClass());

    SpecialSingleSignOn(UvmContext uvmContext, String ... contextPathArray )
    {
        Set<String> contextSet = new HashSet<String>();

        for ( String contextPath : contextPathArray ) contextSet.add( contextPath );

        this.uvmContext = uvmContext;
        this.uvmContextSet =  Collections.unmodifiableSet( contextSet );
    }

    /**
     * Perform single-sign-on support processing for this request.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void invoke(Request request, Response response)
        throws IOException, ServletException
    {
        String contextPath = request.getContextPath();
        if ( uvmContextSet.contains(contextPath)) {
            /* Ignore single sign on for this context path */
            if ( logger.isDebugEnabled()) {
                logger.debug( "The path: [" + contextPath + "] is ignored by single sign on" );
            }

            Valve next = getNext();
            if ( next != null ) next.invoke(request,response);
            return;
        } else {
            if ( logger.isDebugEnabled()) {
                logger.debug( "The path: [" + contextPath + "] uses sign on" );
            }
        }
        super.invoke( request, response );

        Principal principal = request.getUserPrincipal();

        if (principal == null ) {
             logger.debug( "No principal registered for this session." );
        } else {
            logger.debug( "non-portal principal registered for this session: " + principal + "." );
        }
    }

    @Override
    protected void register(String ssoId, Principal principal, String authType, String username, String password)
    {
        /* Never register these sessions, they are bunk */
         if (principal instanceof UvmPrincipal ) {
             if ( logger.isDebugEnabled()) {
                 logger.debug( "Ignoring uvm principal: " + principal + "/" + principal.getClass());
             }

             return;
         }

        if ( logger.isDebugEnabled()) {
            logger.debug( "registering principal: " + principal );
        }

        super.register(ssoId,principal,authType,username,password);
    }
    
    @Override
    public String toString()
    {
        return "SpecialSingleSignOn[uvm]";
    }
}
