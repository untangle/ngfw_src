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
    public UvmException()
    {
        super();
    }

    public UvmException(String message)
    {
        super(message);
    }

    public UvmException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UvmException(Throwable cause)
    {
        super(cause);
    }
}
