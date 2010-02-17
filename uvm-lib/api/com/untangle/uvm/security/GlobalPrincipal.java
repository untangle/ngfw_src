/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.security;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Global Principal, this is principal that is accepted for all
 * servlets except the portal.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 */
public class GlobalPrincipal implements Principal, Serializable
{
    private static final long serialVersionUID = -5067063988261711499L;
    
    /* Global principals are never readonly because they are intended for servlets */
    private final String user;
    private final Date loginDate;

    // constructors -----------------------------------------------------------

    // With domain
    public GlobalPrincipal( String user )
    {
        this.user = user;
        this.loginDate = new Date(System.currentTimeMillis());
    }

    // Principal methods ------------------------------------------------------

    public String getName()
    {
        return this.user;
    }

    public String toString()
    {
        return "GlobalPrincipal of: " + this.user + " on: " + this.loginDate;
    }

    public boolean equals(Object o)
    {
        if ( !this.getClass().isInstance( o )) return false;

        GlobalPrincipal gp = (GlobalPrincipal)o;
        
        return this.user.equals(gp.user)
                && this.loginDate.equals(gp.loginDate);
    }

    public int hashCode()
    {
        if (null == this.user || null == this.loginDate) {
            Logger.getLogger(getClass()).warn( "null in GlobalPrincipal: " + this );
            return 0;
        } else {
            int result = 17;
            result = 37 * result + this.user.hashCode();
            result = 37 * result + this.loginDate.hashCode();

            return result;
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * Gets the user associated with this login.
     */
    public String getUser()
    {
        return this.user;
    }

    /**
     * Date the user logged in.
     *
     * @return the date of login
     */
    public Date getLoginDate()
    {
        return this.loginDate;
    }
}
