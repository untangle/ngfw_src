/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Parser.java,v 1.2 2005/03/17 02:47:47 amread Exp $
 */

package com.metavize.tran.token;


public interface Parser
{
    void newSession(ParseEvent pe);
    TokenStreamer endSession();
    ParseResult parse(ParseEvent pe) throws ParseException;
    void handleTimer(ParseEvent pe);
}
