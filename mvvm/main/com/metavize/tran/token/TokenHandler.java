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
    TokenResult handleClientToken(Token token) throws TokenException;
    TokenResult handleServerToken(Token token) throws TokenException;
    void handleClientFin() throws TokenException;
    void handleServerFin() throws TokenException;
    void handleTimer() throws TokenException;
}
