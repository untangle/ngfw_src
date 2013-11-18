/**
 * $Id$
 */
package com.untangle.node.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Wrapper around a Log4J Logger, which delays String concatination
 * until it is known if a message will reach the actual log file.
 *
 * Works like the log4J version, except Throwables are the
 * <b>first</b> argument to methods if you want to log the exception
 * w/ its stack.  By using JDK 1.5 varargs and auto boxing, you can
 * also pass as many arguments of any type to the logging methods.
 */
public class UtLogger
{
    private Logger logger;

    public UtLogger(Logger logger)
    {
        this.logger = logger;
    }
    
    @SuppressWarnings({"unchecked","rawtypes"})
    public UtLogger(Class category)
    {
        this(Logger.getLogger(category));
    }

    public void debug(Object...msg)
    {
        if(logger.isEnabledFor(Level.DEBUG)) {
            logger.debug(arrayToString(msg));
        }
    }

    public void debug(Throwable t)
    {
        logger.debug(t);
    }

    public void debug(Throwable t, Object...msg)
    {
        if(logger.isEnabledFor(Level.DEBUG)) {
            logger.debug(arrayToString(msg), t);
        }
    }

    public void info(Object...msg)
    {
        if(logger.isEnabledFor(Level.INFO)) {
            logger.info(arrayToString(msg));
        }
    }

    public void info(Throwable t)
    {
        logger.info(t);
    }

    public void info(Throwable t, Object...msg)
    {
        if(logger.isEnabledFor(Level.INFO)) {
            logger.info(arrayToString(msg), t);
        }
    }

    public void warn(Object...msg) {
        if(logger.isEnabledFor(Level.WARN)) {
            logger.warn(arrayToString(msg));
        }
    }

    public void warn(Throwable t)
    {
        logger.warn(t);
    }

    public void warn(Throwable t, Object...msg)
    {
        if(logger.isEnabledFor(Level.WARN)) {
            logger.warn(arrayToString(msg), t);
        }
    }

    public void error(Object...msg)
    {
        logger.error(arrayToString(msg));
    }

    public void error(Throwable t)
    {
        logger.error(t);
    }

    public void error(Throwable t, Object...msg)
    {
        logger.error(arrayToString(msg), t);
    }

    public void fatal(Object...msg)
    {
        logger.fatal(arrayToString(msg));
    }

    public void fatal(Throwable t)
    {
        logger.fatal(t);
    }

    public void fatal(Throwable t, Object...msg)
    {
        logger.fatal(arrayToString(msg), t);
    }

    private String arrayToString(Object...msg)
    {
        StringBuilder sb = new StringBuilder();
        for(Object obj : msg) {
            if(obj == null) {
                sb.append("null");
            }
            else {
                sb.append(obj.toString());
            }
        }
        return sb.toString();
    }
}
