package com.metavize.tran.ids.options;

import java.lang.reflect.*;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSRuleSignature;

public abstract class IDSOption {
	private IDSRuleSignature signature;
	private boolean negationFlag = false;
	
	private static final Logger log = Logger.getLogger(IDSOption.class);
	static {
		log.setLevel(Level.WARN);
	}
				
	protected IDSOption(IDSRuleSignature signature, String params) { 
		
		this.signature = signature;
	}
		
	public IDSRuleSignature getSignature() {
		return signature;
	}
	
	public boolean negationFlag() {
		return negationFlag;
	}
	
	abstract public boolean runnable();
	abstract public boolean run();

	public static IDSOption buildOption(IDSRuleSignature signature, String optionName, String params) {
		
		boolean flag = false;
		if(params.charAt(0) == '!')  {
			flag = true;
			params = params.replaceFirst("!","");
		}

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
			option.negationFlag = flag;
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

