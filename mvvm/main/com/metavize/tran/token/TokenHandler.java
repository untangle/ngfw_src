/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TokenHandler.java,v 1.2 2005/01/11 08:38:51 amread Exp $
 */

package com.metavize.tran.token;

public interface TokenHandler
{
    // XXX what should these throw?
    TokenResult handleClientToken(TokenEvent e);
    TokenResult handleServerToken(TokenEvent e);
    void handleTimer(TokenEvent e);
}
