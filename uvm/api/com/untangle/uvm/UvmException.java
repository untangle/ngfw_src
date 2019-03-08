/**
 * $Id$
 */

package com.untangle.uvm;

/**
 * Base exception class for UVM.
 * 
 */
@SuppressWarnings("serial")
public class UvmException extends Exception
{
    /**
     * Constructor
     */
    public UvmException()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param message
     *        The exception message
     */
    public UvmException(String message)
    {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message
     *        The exception message
     * @param cause
     *        The exception cause
     */
    public UvmException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructor
     * 
     * @param cause
     *        The exception cause
     */
    public UvmException(Throwable cause)
    {
        super(cause);
    }
}
