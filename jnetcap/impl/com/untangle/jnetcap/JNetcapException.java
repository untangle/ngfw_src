/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 * JNetcapException
 */
@SuppressWarnings("serial")
public class JNetcapException extends Exception
{
    /**
     * JNetcapException
     */
    public JNetcapException() 
    { 
        super(); 
    }

    /**
     * JNetcapException
     * @param message
     */
    public JNetcapException( String message )
    { 
        super( message ); 
    }

    /**
     * JNetcapException
     * @param message
     * @param cause
     */
    public JNetcapException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    /**
     * JNetcapException
     * @param cause
     */
    public JNetcapException( Throwable cause )
    { 
        super( cause ); 
    }
}
