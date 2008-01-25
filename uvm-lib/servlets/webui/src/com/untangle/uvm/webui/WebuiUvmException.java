package com.untangle.uvm.webui;

import com.untangle.uvm.UvmException;

/**
 * Signals problem relating to a WebUI UVM client.
 *
 * @author </a>
 * @version 1.0
 */
public class WebuiUvmException extends UvmException {
    public WebuiUvmException()
    {
        super();
    }

    public WebuiUvmException(String message)
    {
        super(message);
    }

    public WebuiUvmException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public WebuiUvmException(Throwable cause)
    {
        super(cause);
    }
}
