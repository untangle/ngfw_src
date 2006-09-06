/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;

public interface TokenHandlerFactory
{
    void handleNewSessionRequest(TCPNewSessionRequest tsr);
    TokenHandler tokenHandler(TCPSession s);
}
