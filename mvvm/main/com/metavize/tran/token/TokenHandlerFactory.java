/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TokenHandlerFactory.java,v 1.5 2005/01/18 05:44:04 amread Exp $
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;

public interface TokenHandlerFactory
{
    TokenHandler tokenHandler(TCPSession s);
}
