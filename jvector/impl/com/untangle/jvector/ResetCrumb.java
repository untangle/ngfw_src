/**
 * $Id: ResetCrumb.java 35567 2013-08-08 07:47:12Z dmorris $
 */
package com.untangle.jvector;

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
