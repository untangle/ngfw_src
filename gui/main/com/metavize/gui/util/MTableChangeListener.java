/*
 * DamageControl.java
 *
 * Created on November 17, 2004, 9:21 PM
 */

package com.metavize.gui.util;

/**
 *
 * @author  inieves
 */
public abstract interface MTableChangeListener {
    
    public abstract void damageControl(Object reference);
    public abstract void dataChangedInvalid(Object reference);
    public abstract void dataChangedValid(Object reference);
    public abstract void dataRefreshed(Object reference);
    
}
