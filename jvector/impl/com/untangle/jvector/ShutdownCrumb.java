/**
 * $Id: ShutdownCrumb.java 35567 2013-08-08 07:47:12Z dmorris $
 */
package com.untangle.jvector;

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

    private ShutdownCrumb() 
    {
    }
    
    public void raze()
    {
    }
    
    public boolean isExpired()
    {
        return ( this == EXPIRED ) ? true : false;
    }

    public int type()
    { 
        return TYPE_SHUTDOWN; 
    }

    public static ShutdownCrumb getInstance() 
    {
        return INSTANCE;
    }

    public static ShutdownCrumb getInstance( boolean isExpired ) 
    {
        return ( isExpired ) ? EXPIRED : INSTANCE;
    }
    
    public static ShutdownCrumb getInstanceExpired()
    {
        return EXPIRED;
    }
}
