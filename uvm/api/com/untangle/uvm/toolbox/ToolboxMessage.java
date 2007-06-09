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

package com.untangle.uvm.toolbox;

import com.untangle.uvm.Message;

public abstract class ToolboxMessage extends Message
{
    public abstract void accept(ToolboxMessageVisitor v);
}
