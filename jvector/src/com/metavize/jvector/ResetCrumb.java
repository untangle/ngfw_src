/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ResetCrumb.java,v 1.2 2005/01/17 21:12:10 rbscott Exp $
 */

package com.metavize.jvector;

public class ResetCrumb extends Crumb
{
    private static final ResetCrumb ACKED     = new ResetCrumb();
    private static final ResetCrumb NOT_ACKED = new ResetCrumb();

    private ResetCrumb()
    {
    }
    
    public void raze()
    {
    }

    /**
     * Determine whether or not this is a reset crumb from an ACKED session
     * or from an unacked session.
     */
    public boolean acked()
    {
        return ( this == ACKED ) ? true : false;
    }

    public int type()
    { 
        return TYPE_RESET;
    }

    /**
     * Get the acked reset crumb.
     */
    public static ResetCrumb getInstance()
    {
        return ACKED;
    }

    /**
     * Get either the acked or non-acked reset crumb 
     */
    public static ResetCrumb getInstance( boolean acked )
    {
        return ( acked ) ? ACKED : NOT_ACKED;
    }

    public static ResetCrumb getInstanceNotAcked() 
    {
        return NOT_ACKED;
    }

}
