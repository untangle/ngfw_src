/**
 * $Id: ParseException.java 32011 2012-05-24 20:50:22Z dmorris $
 */
package com.untangle.uvm.node;

@SuppressWarnings("serial")
public class ParseException extends Exception
{
    public ParseException() 
    { 
        super(); 
    }

    public ParseException( String message )
    { 
        super( message ); 
    }

    public ParseException( String message, Throwable cause )
    { 
        super( message, cause );
    }

    public ParseException( Throwable cause )
    { 
        super( cause ); 
    }
}
