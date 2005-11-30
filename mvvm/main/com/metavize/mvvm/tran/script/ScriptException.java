/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.script;

import com.metavize.mvvm.tran.TransformException;

public class ScriptException extends TransformException {
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
