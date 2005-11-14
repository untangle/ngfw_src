/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

public interface SyslogBuilder
{
    void addField(String key, String value);
    void addField(String key, boolean value);
    void addField(String key, int value);
    void addField(String key, double value);
}
