/**
 * $Id$
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
