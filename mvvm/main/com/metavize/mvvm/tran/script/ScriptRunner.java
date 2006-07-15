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

package com.metavize.mvvm.tran.script;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.TransformException;
import org.apache.log4j.Logger;

public class ScriptRunner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final ScriptRunner INSTANCE = new ScriptRunner();

    private static final String EMPTY_ARGS[] = new String[0];

    private static final String SCRIPT_COMMAND = "sh";

    protected ScriptRunner()
    {
    }

    public final String exec( String scriptName ) throws TransformException
    {
        return exec( scriptName, EMPTY_ARGS );
    }

    /* Done with an array to exec to fix arguments that have spaces. */
    public final String exec( String scriptName, String ... args ) throws TransformException
    {
        String input[] = new String[2 + args.length];
        int c = 0;
        input[c++] = scriptCommand();
        input[c++] = scriptName;
        for ( String arg : args ) {
            if ( arg == null ) {
                logger.error( "NULL argument, using \"\"", new Exception());
                arg = "";
            }

            input[c++] = arg;
        }

        try {
            int code = 0;
            Process p = MvvmContextFactory.context().exec( input );
            StringBuilder sb = new StringBuilder();
            BufferedReader scriptOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (( line = scriptOutput.readLine()) != null ) sb.append( line ).append( "\n" );
            code = p.waitFor();

            if ( code != 0 ) throw new ScriptException( scriptName, code );

            return sb.toString();
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
