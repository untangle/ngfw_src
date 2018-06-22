/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * JVectorException
 */
@SuppressWarnings("serial")
public class JVectorException extends Exception
{
    /**
     * JVectorException
     */
    public JVectorException() 
    { 
        super(); 
    }

    /**
     * JVectorException
     * @param message
     */
    public JVectorException( String message )
    { 
        super( message ); 
    }

    /**
     * JVectorException
     * @param message
     * @param cause
     */
    public JVectorException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    /**
     * JVectorException
     * @param cause
     */
    public JVectorException( Throwable cause )
    { 
        super( cause ); 
    }
}
