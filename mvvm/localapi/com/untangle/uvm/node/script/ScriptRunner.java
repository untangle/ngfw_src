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

package com.untangle.mvvm.tran.script;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tran.TransformException;
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

            flushErrorStream( p, scriptName );

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
        return SCRIPT_COMMAND;
    }

    public static ScriptRunner getInstance()
    {
        return INSTANCE;
    }

    /* Flush out the error stream, and print it out as a warning */
    private void flushErrorStream( Process p, String scriptName ) throws IOException
    {
        BufferedReader scriptError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        boolean isFirst = true;
        String line;
        while (( line = scriptError.readLine()) != null ) {
            if ( isFirst ) logger.info( "[" + scriptName + "] start error output from script." );
            logger.info( "  " + line );
            isFirst = false;
        }
        /* cap of the error message only if there was any error output */
        if ( !isFirst ) logger.info( "[" + scriptName + "] end of error output from script." );
    }
}
