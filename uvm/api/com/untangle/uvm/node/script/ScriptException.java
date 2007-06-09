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

package com.untangle.uvm.node.script;

import com.untangle.uvm.node.NodeException;

public class ScriptException extends NodeException {
    private final int code;
    private final String scriptName;

    ScriptException( String scriptName, int code) 
    { 
        super( "Error executing script [" + scriptName + "]: " + code );
        this.code = code;
        this.scriptName = scriptName;
    }

    public int getCode()
    {
        return this.code;
    }

    public String getScriptName()
    {
        return this.scriptName;
    }
}
