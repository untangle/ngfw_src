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

package com.untangle.uvm.logging;

/**
 * LogMailer emails circular buffers containing log messages when
 * triggered by {@link #sendBuffer()}. This interface allows us to
 * decouple the core of our log4j system from those that need UVM
 * support.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface LogMailer
{
    /**
     * Triggers the LogMailer to send log messages.
     *
     * @param ctx the {@link UvmLoggingContext} that triggered this action.
     */
    void sendBuffer(UvmLoggingContext ctx);
}
