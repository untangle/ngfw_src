/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: LogFormatException.java,v 1.1.1.1 2004/12/01 23:32:21 amread Exp $
 */

package com.metavize.mvvm.logging;

public class LogFormatException extends Exception
{
    public LogFormatException() { }
    public LogFormatException(String m) { super(m); }
    public LogFormatException(String m, Throwable c) { super(m, c); }
    public LogFormatException(Throwable c) { super(c); }
}
