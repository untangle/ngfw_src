/**
 * $Id$
 */

package com.untangle.uvm;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class to read and store stdout, stderr, and the return code from exec() calls
 */
public class ExecManagerResultReader
{
    private static final Logger logger = Logger.getLogger(ExecManagerResultReader.class);

    private Process process;
    private BufferedReader stdoutBufferedReader;
    private BufferedReader stderrBufferedReader;

    /**
     * Constructor
     * 
     * @param process
     *        The exec process
     */
    public ExecManagerResultReader(Process process)
    {
        this.process = process;
        this.stdoutBufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.stderrBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    }

    /**
     * Get the process result
     * 
     * @return The process result
     */
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
     * Wait for a process to finish
     * 
     * @return The exit code of the process
     */
    public int waitFor()
    {
        while (true) {
            try {
                return process.waitFor();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Reads a single line from stdout and returns it. Blocks if not output is
     * available.
     * 
     * @return The line read or null if end of stream
     */
    public String readLineStdout()
    {
        try {
            return this.stdoutBufferedReader.readLine();
        } catch (IOException ex) {
            logger.warn("Failed to read stdout", ex);
            return null;
        }
    }

    /**
     * Reads a single line from stderr and returns it. Blocks if not output is
     * available.
     * 
     * @return The line read or null if end of stream
     */
    public String readLineStderr()
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
     * 
     * @return All data read or null on error
     */
    public String readFromOutput()
    {
        StringBuffer result = new StringBuffer();
        try {
            while (this.stdoutBufferedReader.ready()) {
                char c = (char) this.stdoutBufferedReader.read();
                result.append(c);
            }
        } catch (IOException ex) {
            logger.warn("Failed to read stdout", ex);
        }
        try {
            while (this.stderrBufferedReader.ready()) {
                char c = (char) this.stderrBufferedReader.read();
                result.append(c);
            }
        } catch (IOException ex) {
            logger.warn("Failed to read stderr", ex);
        }

        if (result.length() == 0 && isFinished()) {
            return null;
        }
        return result.toString();
    }

    /**
     * Destroy the process
     */
    public void destroy()
    {
        process.destroy();
    }

    /**
     * See if the process is finished
     * 
     * @return True if finished, otherwise false
     */
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
