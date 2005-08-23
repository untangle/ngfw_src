package com.metavize.tran.ids.options;

import java.lang.reflect.*;
import java.util.regex.PatternSyntaxException;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tran.ParseException;

public abstract class IDSOption {
	protected IDSRuleSignature signature;
	protected boolean negationFlag = false;
	
	private static final Logger log = Logger.getLogger(IDSOption.class);
	static {
		log.setLevel(Level.WARN);
	}
				
	protected IDSOption(IDSRuleSignature signature, String params) { 
		
		this.signature = signature;
	}

	abstract public boolean runnable();
	abstract public boolean run();

	public static IDSOption buildOption(IDSRuleSignature signature, String optionName, String params) {
		
		boolean flag = false;
		if(params.charAt(0) == '!')  {
			flag = true;
			params = params.replaceFirst("!","").trim();
		}

		if(params.charAt(0) == '\"' && params.charAt(params.length()-1) == '\"') 
			params = params.substring(1,params.length()-1);

		IDSOption option = null;
		Class optionDefinition;
		Class[] argsClass = new Class[] { IDSRuleSignature.class, String.class };
		Object[] optionArgs = new Object[] { signature, params };
		Constructor optionConstructor; 
		
		optionName = optionName.toLowerCase();
		char ch = optionName.charAt(0);
		try {
			optionName = optionName.replaceFirst(""+ch,""+(char)(ch - 'a' + 'A'));
		} catch(PatternSyntaxException e) {
			System.out.println("Char:: " + ch); /** ********************/
			System.out.println(optionName);
			System.out.println(params);
		}
		
		try {
			optionDefinition = Class.forName("com.metavize.tran.ids.options."+optionName+"Option");
			optionConstructor = optionDefinition.getConstructor(argsClass);
			option = (IDSOption) createObject(optionConstructor, optionArgs);
			option.negationFlag = flag;
		} catch (ClassNotFoundException e) {
			log.debug("Could not load option(ClassNotFound): "+e.getMessage());
		} catch (NoSuchMethodException e) {
			log.debug("Could not load option(NoSuchMethod): "+e.getMessage());
		}
		return option;
	}
		
	private static Object createObject(Constructor constructor, Object[] arguments) {
		Object object = null;
		try {
			object = constructor.newInstance(arguments);
		} catch (InstantiationException e) {
			log.warn("Could not create object(InstantiationException): "+e.getMessage());
		} catch (IllegalAccessException e) {
			log.warn("Could not create object(IllegalAccessException): "+e.getMessage());
		} catch (IllegalArgumentException e) {
			log.warn("Could not create object(IllegalArgumentException): "+e.getMessage());
		} catch (InvocationTargetException e) {
			log.warn("Could not create object(InvocationTargetException): "+e.getMessage());
			e.printStackTrace();
		}
		return object;
	}
}

