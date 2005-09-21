/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

public class LogFormatException extends Exception
{
    public LogFormatException() { }
    public LogFormatException(String m) { super(m); }
    public LogFormatException(String m, Throwable c) { super(m, c); }
    public LogFormatException(Throwable c) { super(c); }
}
