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

package com.untangle.mvvm;

import java.util.List;

/**
 * An interface for receiving messages.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see com.untangle.mvvm.client.MessageClient
 */
public interface MessageQueue<M extends Message>
{
    /**
     * Get the undelivered messages for this queue.
     *
     * @return outstanding messages.
     */
    List<M> getMessages();
}
