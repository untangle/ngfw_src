package com.metavize.tran.ids;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.tran.token.TokenAdaptor;

public class IDSTransformImpl extends AbstractTransform implements IDSTransform {
	
	private static final Logger log = Logger.getLogger(IDSTransformImpl.class);
	private IDSRules rules = new IDSRules();
	
	static {                        
		log.setLevel(Level.INFO);
	}   
			 
	private final EventHandler handler;
	private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
	private final PipeSpec[] pipeSpecs;

	//private final IDSTest test = new IDSTest();
	
	public IDSTransformImpl() {
		handler = new EventHandler();
	 	octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
		httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
		pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };
	}

	protected PipeSpec[] getPipeSpecs() {
		log.debug("Getting PipeSpec");
		return pipeSpecs;
	}

	private void visitAllFiles(File file) {
		if (file.isDirectory()) {
			String[] children = file.list();
			for (int i=0; i<children.length; i++) 
				visitAllFiles(new File(file, children[i]));
		} 
		else 
			processFile(file);
	}
	
	private void processFile(File file) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String str;
			while ((str = in.readLine()) != null) {
				IDSDetectionEngine.instance().addRule(str.trim());
			}
			in.close();
		} catch (IOException e) { e.printStackTrace(); }
	}

	protected void postInit(String args[]) {
/*		log.info("\n*******************Loading Rules**********************");
		String path =  System.getProperty("bunnicula.home");
		File file = new File(path+"/ids-transform/rules");
		visitAllFiles(file); // */
	}

	protected void preStart() throws TransformStartException {
		IDSTest test = new IDSTest();
		if(!test.runTest())
		  throw new TransformStartException("IDS Test failed");
		
	}
	
	public void reconfigure() throws TransformException {
	}

	//XXX soon to be deprecated ------------------------------------------
	
	public Object getSettings() {
		return null;
	}

	public void setSettings(Object obj) {
	}
}
