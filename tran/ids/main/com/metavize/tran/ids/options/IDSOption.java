package com.metavize.tran.ids.options;

import java.lang.reflect.*;
import java.util.regex.PatternSyntaxException;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;
import com.metavize.mvvm.tran.ParseException;

public abstract class IDSOption {
    protected IDSRuleSignature signature;
    protected boolean negationFlag = false;
	
    private static final Logger log = Logger.getLogger(IDSOption.class);
				
    protected IDSOption(IDSRuleSignature signature, String params) { 
		
        this.signature = signature;
    }

    // Overriden in concrete children that are runnable
    public boolean runnable() {
        return false;
    }

    // Overriden in concrete children that are runnable
    public boolean run(IDSSessionInfo sessionInfo) {
        return true;
    }

    public static IDSOption buildOption(IDSRuleSignature signature, String optionName, String params,
                                        boolean initializeSettingsTime) {
		
        boolean flag = false;
        if(params.charAt(0) == '!')  {
            flag = true;
            params = params.replaceFirst("!","").trim();
        }

        if(params.charAt(0) == '\"' && params.charAt(params.length()-1) == '\"') 
            params = params.substring(1,params.length()-1);

        IDSOption option = null;
        Class optionDefinition;
        Class[] threeArgsClass = new Class[] { IDSRuleSignature.class, String.class, Boolean.TYPE };
        Object[] threeOptionArgs = new Object[] { signature, params, initializeSettingsTime };
        Class[] twoArgsClass = new Class[] { IDSRuleSignature.class, String.class };
        Object[] twoOptionArgs = new Object[] { signature, params };
        Constructor optionConstructor; 
		
        optionName = optionName.toLowerCase();
        char ch = optionName.charAt(0);
        try {
            optionName = optionName.replaceFirst(""+ch,""+(char)(ch - 'a' + 'A'));
        } catch(PatternSyntaxException e) { 
            log.error("Bad option name", e);
        }
		
        try {
            // First look for a three arg one, then the two arg one (since most don't care about
            // initializeSettingsTime).
            optionDefinition = Class.forName("com.metavize.tran.ids.options."+optionName+"Option");
log.debug("option class: " + optionDefinition);

            try {
                optionConstructor = optionDefinition.getConstructor(threeArgsClass);
                option = (IDSOption) createObject(optionConstructor, threeOptionArgs);
            } catch (NoSuchMethodException e) {
                optionConstructor = optionDefinition.getConstructor(twoArgsClass);
                option = (IDSOption) createObject(optionConstructor, twoOptionArgs);
            }
            option.negationFlag = flag;
        } catch (ClassNotFoundException e) {
            log.info("Could not load option(ClassNotFound): " + optionName + ", ignoring rule: " + signature.rule().getText());
            signature.remove(true);
        } catch (NoSuchMethodException e) {
            log.error("Could not load option(NoSuchMethod): ", e);
        }
        return option;
    }
		
    private static Object createObject(Constructor constructor, Object[] arguments) {
        Object object = null;
        try {
            object = constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            log.error("Could not create object(InstantiationException): ", e);
        } catch (IllegalAccessException e) {
            log.error("Could not create object(IllegalAccessException): ", e);
        } catch (IllegalArgumentException e) {
            log.error("Could not create object(IllegalArgumentException): ", e);
        } catch (InvocationTargetException e) {
            log.error("Could not create object(InvocationTargetException): ", e);
        }
        return object;
    }
}

