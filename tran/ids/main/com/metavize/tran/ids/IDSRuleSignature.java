package com.metavize.tran.ids;

import java.lang.reflect.*;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.options.*;

public class IDSRuleSignature {
	
	private List<IDSOption> options = new Vector<IDSOption>();
	private IDSSessionInfo info;
	
	private String message = "No message set";
	private int action;
	
	private static final Logger log = Logger.getLogger(IDSRuleSignature.class);
	static {
		log.setLevel(Level.WARN);
	}
	public IDSRuleSignature(int action) {
		this.action = action;
	}

	public IDSSessionInfo getSessionInfo() {
		return info;
	}

	public void addOption(String optionName, String params) {
		IDSOption option = IDSOption.crazyFoo(this,optionName,params);
		if(option != null)
			options.add(option);
		else
			log.info("No option found: " + optionName);
	}

	public IDSOption getOption(String name) {
		Iterator<IDSOption> it = options.iterator();
		Class optionDefinition = null;
		try {
			optionDefinition = Class.forName("com.metavize.tran.ids.options."+name);
		} catch (ClassNotFoundException e) {
			log.error("Could not load option: " + e.getMessage());
		}
		while(it.hasNext()) {
			IDSOption option = it.next();
			if(optionDefinition.isInstance(option))
				return option;
		}
		return null;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	public void execute(IDSSessionInfo info) {
		this.info = info;
		
		Iterator<IDSOption> it = options.iterator();
		while(it.hasNext()) {
			IDSOption option = it.next();
			if(!option.run())
				return;
		}
	//	doAction();
	}

	private void doAction() {
		switch(action) {
			case IDSRules.ALERT:
				System.out.println(message);
				break;
			case IDSRules.LOG:
				break;
		}
	}
}
