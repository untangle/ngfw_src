/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
