/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * ShutdownCrumb - this crumb represents a shutdown event
 */
public class ShutdownCrumb extends Crumb
{
    /**
     * A normal shutdown crumb 
     */
    private static final ShutdownCrumb INSTANCE = new ShutdownCrumb();

    /**
     * An expired/dead session shutdown crumb.  In an expired session, vectoring
     * is completed, and it is infeasible to send and more crumbs 
     */
    private static final ShutdownCrumb EXPIRED  = new ShutdownCrumb();

    /**
     * ShutdownCrumb - private
     */
    private ShutdownCrumb() 
    {
    }
    
    /**
     * raze - noop
     */
    public void raze()
    {
    }
    
    /**
     * isExpired
     * @return
     */
    public boolean isExpired()
    {
        return ( this == EXPIRED ) ? true : false;
    }

    /**
     * type
     * @return
     */
    public int type()
    { 
        return TYPE_SHUTDOWN; 
    }

    /**
     * getInstance
     * @return
     */
    public static ShutdownCrumb getInstance() 
    {
        return INSTANCE;
    }

    /**
     * getInstance
     * @param isExpired
     * @return
     */
    public static ShutdownCrumb getInstance( boolean isExpired ) 
    {
        return ( isExpired ) ? EXPIRED : INSTANCE;
    }
    
    /**
     * getInstanceExpired
     * @return
     */
    public static ShutdownCrumb getInstanceExpired()
    {
        return EXPIRED;
    }
}
