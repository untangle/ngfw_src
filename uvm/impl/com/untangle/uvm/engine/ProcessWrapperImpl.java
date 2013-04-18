package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.untangle.uvm.ProcessWrapper;

public class ProcessWrapperImpl implements ProcessWrapper 
{

	private Process process;

	public ProcessWrapperImpl(Process process) {
		this.process = process;
	}

	public void destroy() {
		process.destroy();
	}

	public int exitValue() {
		int retVal = -1;
		try {
			retVal = process.exitValue();
		} catch (IllegalThreadStateException ex) {
			// process hasn't exited
		}
		return retVal;
	}

	public int waitFor() throws InterruptedException {
		return process.waitFor();
	}

	public InputStream getErrorStream() {
		return process.getErrorStream();
	}

	public InputStream getInputStream() {
		return process.getInputStream();
	}

	public OutputStream getOutputStream() {
		return process.getOutputStream();
	}

	@Override
	public String getOuptut() {
		StringBuffer result = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		try {
			while (br.ready()) {
				String s = br.readLine();
				if ( s != null) {
					result.append(s).append(System.getProperty("line.separator"));
				} else {
					break;
				}
			}
		} catch ( IOException ex) {
			ex.printStackTrace();
		}
		return result.toString();
	}
	
	

}
