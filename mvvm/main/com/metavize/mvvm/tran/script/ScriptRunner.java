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

package com.metavize.mvvm.tran.script;

import com.metavize.mvvm.tran.TransformException;

public class ScriptRunner
{
    private static final ScriptRunner INSTANCE = new ScriptRunner();

    private static final String EMPTY_ARGS[] = new String[0];

    private static final String SCRIPT_COMMAND = "sh";

    protected ScriptRunner()
    {
    }

    public final void exec( String scriptName ) throws TransformException
    {
        exec( scriptName, EMPTY_ARGS );
    }

    /* Done with an array to exec to fix arguments that have spaces. */
    public final void exec( String scriptName, String ... args ) throws TransformException
    {
        String input[] = new String[2 + args.length];
        int c = 0;
        input[c++] = scriptCommand();
        input[c++] = scriptName;
        for ( String arg : args ) input[c++] = arg;
    
        try {
            int code = 0;
            Process p = Runtime.getRuntime().exec( input );
            code = p.waitFor();
            
            if ( code != 0 ) throw new TransformException( "Error executing script [" + scriptName + "]: " 
                                                           + code );
        } catch ( TransformException e ) {
            throw e;
        } catch( Exception e ) {
            throw new TransformException( "Error executing script [" + scriptName + "]", e );
        }
    }

    protected String scriptCommand()
    {
        return "sh";
    }

    public static ScriptRunner getInstance()
    {
        return INSTANCE;
    }
}
