/**
 * $Id$
 */
package com.untangle.uvm.node.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;

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
            String output = "";
            String cmdStr = new String();
            for (int i = 0 ; i < input.length; i++) {
                cmdStr = cmdStr.concat("\"" + input[i] + "\"" + " ");
            }

            ExecManagerResult result = UvmContextFactory.context().execManager().exec(cmdStr);
            
            code = result.getResult();
            output = result.getOutput();

            if ( code != 0 )
                throw new ScriptException( scriptName + " returned: " + code, code );

            return output;
        } catch( Exception e ) {
            logger.warn( "Error executing script [" + scriptName + "]", e );
            return "";
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
