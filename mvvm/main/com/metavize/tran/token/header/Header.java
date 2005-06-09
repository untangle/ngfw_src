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

package com.metavize.tran.token.header;

import com.metavize.tran.token.Token;

public interface Header extends Token
{
    void addField(Field field) throws IllegalFieldException;

    void setField(String key, String value) throws IllegalFieldException;
}
