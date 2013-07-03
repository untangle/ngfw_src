/**
 * $Id: ExecManagerResultReader.java,v 1.00 2013/04/19 23:04:51 vdumitrescu Exp $
 */
package com.untangle.uvm;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecManagerResultReader
{
    private static final Logger logger = Logger.getLogger( ExecManagerResultReader.class );

    private Process process;
    private BufferedReader stdoutBufferedReader;
    private BufferedReader stderrBufferedReader;
        
    public ExecManagerResultReader(Process process)
    {
        this.process = process;
        this.stdoutBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.stderrBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }
    
    public Integer getResult()
    {
        int retVal = -1;
        try {
            retVal = process.exitValue();
        } catch (IllegalThreadStateException ex) {
            // process hasn't exited
        }
        return retVal;
    }
    
    /**
     * Reads a single line from stdout and returns it.
     * Blocks if not output is available
     */
    public String readlineStdout()
    {
        try {
            return this.stdoutBufferedReader.readLine();
        } catch (IOException ex) {
            logger.warn("Failed to read stdout", ex);
            return null;
        }
    }

    /**
     * Reads a single line from stderr and returns it.
     * Blocks if not output is available
     */
    public String readlineStderr()
    {
        try {
            return this.stderrBufferedReader.readLine();
        } catch (IOException ex) {
            logger.warn("Failed to read stderr", ex);
            return null;
        }
    }
    
    /**
     * Reads both all available stdout and stderr and returns output
     */
    public String readFromOutput()
    {
        StringBuffer result = new StringBuffer();
        try {
            while (this.stdoutBufferedReader.ready()) {
                String s = this.stdoutBufferedReader.readLine();
                if (s != null) {
                    result.append(s).append(System.getProperty("line.separator"));
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            logger.warn("Failed to read stdout", ex);
        }
        try {
            while (this.stderrBufferedReader.ready()) {
                String s = this.stderrBufferedReader.readLine();
                if (s != null) {
                    result.append(s).append(System.getProperty("line.separator"));
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            logger.warn("Failed to read stderr", ex);
        }

        if(result.length()==0 && isFinished()) {
            return null;
        }
        return result.toString();
    }

    public void destroy()
    {
        process.destroy();
    }

    private boolean isFinished()
    {
        try {
            process.exitValue();
        } catch (IllegalThreadStateException ex) {
            // process hasn't exited
            return false;
        }
        return true;
    }
}
