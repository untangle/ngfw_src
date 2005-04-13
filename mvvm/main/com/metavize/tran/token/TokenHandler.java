/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

public interface TokenHandler
{
    // XXX what should these throw?
    TokenResult handleClientToken(Token token);
    TokenResult handleServerToken(Token token);
    void handleTimer();
}
