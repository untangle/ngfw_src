/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * ResetCrumb represents a reset event (for TCPSource/TCPSink)
 */
public class ResetCrumb extends Crumb
{
    private static final ResetCrumb ACKED     = new ResetCrumb();
    private static final ResetCrumb NOT_ACKED = new ResetCrumb();
    
    /**
     * ResetCrumb
     */
    private ResetCrumb()
    {
    }
    
    /**
     * raze - noop
     */
    public void raze()
    {
    }

    /**
     * acked
     * Determine whether or not this is a reset crumb from an ACKED
     * session or from an unacked session.
     * @return
     */
    public boolean acked()
    {
        return ( this == ACKED ) ? true : false;
    }

    /**
     * type
     * @return
     */
    public int type()
    { 
        return TYPE_RESET;
    }

    /**
     * getInstance
     * Get the acked reset crumb.
     * @return
     */
    public static ResetCrumb getInstance()
    {
        return ACKED;
    }

    /**
     * getInstance
     * Get either the acked or non-acked reset crumb
     * @param acked
     * @return
     */
    public static ResetCrumb getInstance( boolean acked )
    {
        return ( acked ) ? ACKED : NOT_ACKED;
    }

    /**
     * getInstanceNotAcked
     * @return
     */
    public static ResetCrumb getInstanceNotAcked() 
    {
        return NOT_ACKED;
    }

}
