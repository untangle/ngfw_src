package com.untangle.node.web_cache; // IMPL

import org.apache.log4j.Logger;

public class WebCacheStackDump
{
    public static void error(Logger logger,String cname,String fname,Throwable t)
    {
    StackTraceElement[] stack = t.getStackTrace();
    String message = "EXCEPTION (" + t + ") in " + cname + "." + fname + "\n";
    message = message + "  MESSAGE: " + t.getMessage() + " CAUSE: " + t.getCause() + "\n";

        for(int x = 0;x < stack.length;x++)
        {
        message = message + "  STACK(" + x + ") " + stack[x].toString() + "\n";
        }

    logger.error(message);
    }

    public static void display(Logger logger,String cname,String fname)
    {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    String message = "STACK DISPLAY of " + cname + "." + fname + "\n";

            for(int x = 0;x < stack.length;x++)
            {
            message = message + "  STACK(" + x + ") " + stack[x].toString() + "\n";
            }

    logger.debug(message);
    }
}
