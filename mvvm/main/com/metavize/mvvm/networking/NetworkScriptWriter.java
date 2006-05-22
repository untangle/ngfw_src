/*
 * Copyright (c) 2003, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.tran.script.ScriptWriter;
import org.apache.log4j.Logger;

import static com.metavize.mvvm.tran.script.ScriptWriter.COMMENT;
import static com.metavize.mvvm.tran.script.ScriptWriter.METAVIZE_HEADER;

class NetworkScriptWriter extends ScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String NETWORK_HEADER =
        COMMENT + METAVIZE_HEADER + "\n" +
        COMMENT + " Network Spaces Configuration Script\n\n";

    NetworkScriptWriter()
    {
        super();
    }



}
