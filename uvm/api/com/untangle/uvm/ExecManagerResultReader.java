/**
 * $Id: ExecManagerResultReader.java,v 1.00 2013/04/19 23:04:51 vdumitrescu Exp $
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecManagerResultReader
{
    private Process process;

    public ExecManagerResultReader(Process process)
    {
        this.process = process;
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
    
    public String readFromOutput()
    {
        StringBuffer result = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            while (br.ready()) {
                String s = br.readLine();
                if (s != null) {
                    result.append(s).append(System.getProperty("line.separator"));
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        try {
            while (br.ready()) {
                String s = br.readLine();
                if (s != null) {
                    result.append(s).append(System.getProperty("line.separator"));
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
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
