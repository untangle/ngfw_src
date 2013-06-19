/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
    private Logger m_logger;

    public UtLogger(Logger logger) {
        m_logger = logger;
    }
    
    @SuppressWarnings("unchecked")
    public UtLogger(Class category) {
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
