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

package com.untangle.uvm.logging;

import com.untangle.uvm.node.IPaddr;
import java.net.InetAddress;
import java.util.Date;

public interface SyslogBuilder
{
    void startSection(String s);

    void addField(String key, String value);
    void addField(String key, boolean value);
    void addField(String key, int value);
    void addField(String key, long value);
    void addField(String key, double value);
    void addField(String key, InetAddress addr);
    void addField(String key, IPaddr addr);
    void addField(String key, Date date);
}
