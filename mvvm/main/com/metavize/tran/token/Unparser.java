/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Unparser.java,v 1.3 2005/03/17 02:47:47 amread Exp $
 */

package com.metavize.tran.token;

public interface Unparser
{
    void newSession(UnparseEvent ue);
    TokenStreamer endSession();
    UnparseResult unparse(UnparseEvent ue) throws UnparseException;
}
