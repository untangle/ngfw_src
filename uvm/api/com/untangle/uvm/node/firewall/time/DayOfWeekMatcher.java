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

package com.untangle.uvm.node.firewall.time;

import java.util.Date;


public interface DayOfWeekMatcher
{
    public boolean isMatch( Date when );

    public String toDatabaseString();
}
