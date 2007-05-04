/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.util;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * Wrapper around a Log4J Logger, which
 * delays String concatination until
 * it is known if a message will reach the
 * actual log file.
 * <br><br>
 * Works like the log4J version, except Throwables are
 * the <b>first</b> argument to methods if you want to
 * log the exception w/ its stack.  By using JDK 1.5
 * varargs and auto boxing, you can also pass as many
 * arguments of any type to the logging methods.
 */
public class MVLogger {

    private Logger m_logger;

    public MVLogger(Logger logger) {
        m_logger = logger;
    }
    public MVLogger(Class category) {
        this(Logger.getLogger(category));
    }

    public void debug(Object...msg) {
        if(m_logger.isEnabledFor(Level.DEBUG)) {
            m_logger.debug(arrayToString(msg));
        }
    }

    public void debug(Throwable t) {
        m_logger.debug(t);
    }

    public void debug(Throwable t, Object...msg) {
        if(m_logger.isEnabledFor(Level.DEBUG)) {
            m_logger.debug(arrayToString(msg), t);
        }
    }

    public void info(Object...msg) {
        if(m_logger.isEnabledFor(Level.INFO)) {
            m_logger.info(arrayToString(msg));
        }
    }

    public void info(Throwable t) {
        m_logger.info(t);
    }

    public void info(Throwable t, Object...msg) {
        if(m_logger.isEnabledFor(Level.INFO)) {
            m_logger.info(arrayToString(msg), t);
        }
    }


    public void warn(Object...msg) {
        if(m_logger.isEnabledFor(Level.WARN)) {
            m_logger.warn(arrayToString(msg));
        }
    }

    public void warn(Throwable t) {
        m_logger.warn(t);
    }

    public void warn(Throwable t, Object...msg) {
        if(m_logger.isEnabledFor(Level.WARN)) {
            m_logger.warn(arrayToString(msg), t);
        }
    }

    public void error(Object...msg) {
        m_logger.error(arrayToString(msg));
    }

    public void error(Throwable t) {
        m_logger.error(t);
    }

    public void error(Throwable t, Object...msg) {
        m_logger.error(arrayToString(msg), t);
    }

    public void fatal(Object...msg) {
        m_logger.fatal(arrayToString(msg));
    }

    public void fatal(Throwable t) {
        m_logger.fatal(t);
    }

    public void fatal(Throwable t, Object...msg) {
        m_logger.fatal(arrayToString(msg), t);
    }




    private String arrayToString(Object...msg) {
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
