/**
 * $Id: ExecResultReaderImpl.java,v 1.00 2013/04/19 23:04:51 vdumitrescu Exp $
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.untangle.uvm.ExecResultReader;

public class ExecResultReaderImpl implements ExecResultReader 
{

	private Process process;

	public ExecResultReaderImpl(Process process)
    {
		this.process = process;
	}
	
	@Override
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
	
	@Override
	public String readFromOutput() {
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

	@Override
	public void destroy()
    {
		process.destroy();
	}

	private boolean isFinished(){
		try {
			process.exitValue();
		} catch (IllegalThreadStateException ex) {
			// process hasn't exited
			return false;
		}
		return true;
	}

}
