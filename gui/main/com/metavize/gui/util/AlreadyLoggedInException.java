/*
 * UpgradeException.java
 *
 * Created on August 16, 2004, 11:12 AM
 */

package com.metavize.gui.util;




public class AlreadyLoggedInException extends Exception {
    
    public AlreadyLoggedInException(String name){
        super(name);
    }
}
