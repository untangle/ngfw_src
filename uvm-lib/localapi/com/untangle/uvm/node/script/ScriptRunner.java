/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;

public class ScriptRunner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final ScriptRunner INSTANCE = new ScriptRunner();

    private static final String EMPTY_ARGS[] = new String[0];

    private static final String SCRIPT_COMMAND = "bash";

    protected ScriptRunner()
    {
    }

    public final String exec( String scriptName ) throws ScriptException
    {
        return exec( scriptName, EMPTY_ARGS );
    }

    /* Done with an array to exec to fix arguments that have spaces. */
    public final String exec( String scriptName, String ... args ) throws ScriptException
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
            Process p = LocalUvmContextFactory.context().exec( input );
            StringBuilder sb = new StringBuilder();
            BufferedReader scriptOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (( line = scriptOutput.readLine()) != null ) sb.append( line ).append( "\n" );

            flushErrorStream( p, scriptName );

            code = p.waitFor();

            if ( code != 0 )
                throw new ScriptException( scriptName + "returned: ", code );

            return sb.toString();
        } catch( Exception e ) {
            logger.warn( "Error executing script [" + scriptName + "]", e );
            return null;
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

    @SuppressWarnings("serial")
    public class ScriptException extends Exception
    {
        int code;
        
        public ScriptException(String str, int code)
        {
            super(str);
            this.code = code;
        }

        public int getCode()
        {
            return this.code;
        }
    }
}
