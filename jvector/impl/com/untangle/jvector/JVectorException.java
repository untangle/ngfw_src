/**
 * $Id: JVectorException.java 35567 2013-08-08 07:47:12Z dmorris $
 */
package com.untangle.jvector;

@SuppressWarnings("serial")
public class JVectorException extends Exception
{
    public JVectorException() 
    { 
        super(); 
    }

    public JVectorException( String message )
    { 
        super( message ); 
    }

    public JVectorException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public JVectorException( Throwable cause )
    { 
        super( cause ); 
    }
}
