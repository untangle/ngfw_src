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

package com.untangle.tran.token;

public interface TokenHandler
{
    // XXX what should these throw?
    TokenResult handleClientToken(Token token) throws TokenException;
    TokenResult handleServerToken(Token token) throws TokenException;

    /**
     * Handle a FIN from the client.  By default, this FIN
     * does <b>not</b> result in the shutdown of the server.
     * To accomplish this (to logically propigate the
     * shutdown) call <code>m_myTCPSession.shutdownServer()</code>
     */
    void handleClientFin() throws TokenException;
    void handleServerFin() throws TokenException;
    void handleTimer() throws TokenException;
    void handleFinalized() throws TokenException;

    TokenResult releaseFlush();
}
