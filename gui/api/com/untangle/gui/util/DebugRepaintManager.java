/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.untangle.gui.util;

import java.awt.Component;
import java.io.PrintStream;
import java.io.PrintWriter;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

public class DebugRepaintManager extends RepaintManager {

    public DebugRepaintManager(){
        super();
        // No need for this to show up in the console for users.
        // System.err.println("[!!Running Debug Repaint Manager!!]");
    }

    private final Logger logger = Logger.getLogger(getClass());

    public synchronized void addInvalidComponent(JComponent component){
        checkEDTRule(component);
        super.addInvalidComponent(component);
    }

    public synchronized void addDirtyRegion(JComponent component, int x, int y, int w, int h){
        checkEDTRule(component);
        super.addDirtyRegion(component, x, y, w, h);
    }

    protected void checkEDTRule(Component component){
        if( violatesEDTRule(component) ){
            EDTRuleViolation violation = new EDTRuleViolation(component);
            StackTraceElement[] stackTrace = violation.getStackTrace();
            try{
                for(int e = stackTrace.length-1; e >= 0; e--){
                    if(isLiableToEDTRule(stackTrace[e])){
                        StackTraceElement[] subStackTrace = new StackTraceElement[stackTrace.length-e];
                        System.arraycopy(stackTrace, e, subStackTrace, 0, subStackTrace.length);
                        violation.setStackTrace(subStackTrace);
                    }
                }
            }
            catch(Exception e){
                // keep stack trace
            }
            indicate(violation);
        }
    }

    protected boolean violatesEDTRule(Component component){
        return !SwingUtilities.isEventDispatchThread() && component.isShowing();
    }

    protected boolean isLiableToEDTRule(StackTraceElement element) throws Exception {
        return Component.class.isAssignableFrom(Class.forName(element.getClassName()));
    }

    protected void indicate(EDTRuleViolation violation){
        violation.printStackTrace();
    }

}

class EDTRuleViolation extends Exception{

    private Component component;

    public EDTRuleViolation(Component component){
        this.component = component;
    }

    public void printStackTrace(){
        printStackTrace(System.err);
    }

    public void printStackTrace(PrintStream s)
    {
        s.println("");
        s.println("[ EDT RULE VIOLATION ]");
        super.printStackTrace(s);
        s.println("[" + component.toString() + "]");
    }

    public void printStackTrace(PrintWriter w)
    {
        w.println("");
        w.println("[ EDT RULE VIOLATION ]");
        super.printStackTrace(w);
        w.println("[" + component.toString() + "]");
    }
}
