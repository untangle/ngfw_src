/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: LogMailer.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.mvvm.logging;


public interface LogMailer
{
    void sendBuffer(MvvmLoggingContext ctx);
    void sendMessage(MvvmLoggingContext ctx);
}
