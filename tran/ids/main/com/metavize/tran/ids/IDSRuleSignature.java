package com.metavize.tran.ids;

import java.lang.reflect.*;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.ListIterator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.ids.options.*;

public class IDSRuleSignature {
	
	/***************************************
	 * These are options that are safe to ignore
	 * Any other option *WILL DROP THE RULE*
	 * 
	 * These rules should all be added at some point!
	 *****************************************/
	private String[] ignoreSafeOptions = { "rev","sid","classtype","reference" };
	/** **************************************/
	
	private List<IDSOption> options = new Vector<IDSOption>();
	private IDSSessionInfo info;
	
	private String toString = "Starting..";
	private String message = "No message set";
	private int action;
	private boolean removeFlag = false;
	
	private final Logger eventLog = MvvmContextFactory.context().eventLogger();
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

	public void remove(boolean remove) {
		removeFlag = remove;
	}

	public boolean remove() {
		return removeFlag;
	}
	public void addOption(String optionName, String params) {
		 for(int i = 0; i < ignoreSafeOptions.length; i++) {
			 if(optionName.equalsIgnoreCase(ignoreSafeOptions[i]))
				 return;
		 }
		 
		IDSOption option = IDSOption.buildOption(this,optionName,params);
		if(option != null && option.runnable())
			options.add(option);
		else if(option == null) {
			log.info("Could not add option: " + optionName);
			removeFlag = true;
		}
	}

	public IDSOption getOption(String name, IDSOption callingOption) {
		/**Have to iterate backwards over the options so that options that 
		 * act as modifiers will modify the correct option
		 * eg, in situations where there are multiple content options.
		 */
		int index = options.indexOf(callingOption);
		index = (index < 0) ? options.size():index;

		ListIterator<IDSOption> it = options.listIterator(index);
		Class optionDefinition = null;
		try {
			optionDefinition = Class.forName("com.metavize.tran.ids.options."+name);
		} catch (ClassNotFoundException e) {
			log.error("Could not load option: " + e.getMessage());
		}
		while(it.hasPrevious()) {
			IDSOption option = it.previous();
			if(optionDefinition.isInstance(option))
				return option;
		}
		return null;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	public boolean execute(IDSSessionInfo info) {
		this.info = info;
		
		Iterator<IDSOption> it = options.iterator();
		while(it.hasNext()) {
			IDSOption option = it.next();
			if(!option.run())
				return false;
		}
		doAction();
		return true;
	}

	private void doAction() {
		switch(action) {
			case IDSRuleManager.ALERT:
				//System.out.println(message);
				break;
			case IDSRuleManager.LOG:
				break;
		}
		int id = (info.getSession() == null) ? -1 : info.getSession().id();
		eventLog.info(new IDSLogEvent(id,message,false));
	}

	public void setToString(String string) {
		toString = string;
	}
	
	public String toString() {
		return toString;
	}
}
