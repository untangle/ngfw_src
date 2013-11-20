/**
 * $Id$
 */
package com.untangle.jnetcap;

@SuppressWarnings("serial")
public class JNetcapException extends Exception
{
    public JNetcapException() 
    { 
        super(); 
    }

    public JNetcapException( String message )
    { 
        super( message ); 
    }

    public JNetcapException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public JNetcapException( Throwable cause )
    { 
        super( cause ); 
    }
}
