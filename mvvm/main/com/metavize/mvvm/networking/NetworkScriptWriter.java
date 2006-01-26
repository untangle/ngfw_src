/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.script.ScriptWriter;

import static com.metavize.mvvm.tran.script.ScriptWriter.COMMENT;
import static com.metavize.mvvm.tran.script.ScriptWriter.METAVIZE_HEADER;

class NetworkScriptWriter extends ScriptWriter
{
    private static final Logger logger = Logger.getLogger( NetworkScriptWriter.class );
    
    private static final String NETWORK_HEADER = 
        COMMENT + METAVIZE_HEADER + "\n" +
        COMMENT + " Network Spaces Configuration Script\n\n";

    NetworkScriptWriter()
    {
        super();
    }

    

}
