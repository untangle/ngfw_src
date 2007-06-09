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

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;

public interface TokenHandlerFactory
{
    void handleNewSessionRequest(TCPNewSessionRequest tsr);
    TokenHandler tokenHandler(TCPSession s);
}
