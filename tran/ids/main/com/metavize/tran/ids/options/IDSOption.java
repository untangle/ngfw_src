package com.metavize.tran.ids.options;

import java.lang.reflect.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSRuleSignature;

public abstract class IDSOption {
//Might want to add a runnable flag to remove it from the Signature iterator	
	private static final Logger log = Logger.getLogger(IDSOption.class);
	protected IDSRuleSignature signature;
	
	static {
		log.setLevel(Level.WARN);
	}
				
	protected IDSOption(IDSRuleSignature signature, String params) { 
		this.signature = signature;
	}
	
	abstract public boolean run();

	public static IDSOption crazyFoo(IDSRuleSignature signature, String optionName, String params) {
		IDSOption option = null;
		
		Class optionDefinition;
		Class[] argsClass = new Class[] { IDSRuleSignature.class, String.class };
		Object[] optionArgs = new Object[] { signature, params };
		Constructor optionConstructor; 
		
		optionName = optionName.toLowerCase();
		char ch = optionName.charAt(0);
		optionName = optionName.replaceFirst(""+ch,""+(char)(ch - 'a' + 'A'));
		
		try {
			optionDefinition = Class.forName("com.metavize.tran.ids.options."+optionName+"Option");
			optionConstructor = optionDefinition.getConstructor(argsClass);
			option = (IDSOption) createObject(optionConstructor, optionArgs);
		} catch (ClassNotFoundException e) {
			log.debug("Could not load option: "+e.getMessage());
		} catch (NoSuchMethodException e) {
			log.debug("Could not load option: "+e.getMessage());
		}
		return option;
	}
		
	private static Object createObject(Constructor constructor, Object[] arguments) {
		Object object = null;
		try {
			object = constructor.newInstance(arguments);
			return object;
		} catch (InstantiationException e) {
			log.debug("Could not create object: "+e.getMessage());
		} catch (IllegalAccessException e) {
			log.debug("Could not create object: "+e.getMessage());
		} catch (IllegalArgumentException e) {
			log.debug("Could not create object: "+e.getMessage());
		} catch (InvocationTargetException e) {
			log.debug("Could not create object: "+e.getMessage());
		}
		return object;
	}
}

