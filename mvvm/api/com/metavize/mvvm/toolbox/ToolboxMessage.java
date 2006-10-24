/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.toolbox;

import com.metavize.mvvm.Message;

public abstract class ToolboxMessage extends Message
{
    public abstract void accept(ToolboxMessageVisitor v);
}
