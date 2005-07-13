/*
 * MCasingJPanel.java
 *
 * Created on July 12, 2005, 5:35 PM
 */

package com.metavize.gui.transform;

import com.metavize.mvvm.tran.TransformContext;

/**
 *
 * @author inieves
 */
public abstract class MCasingJPanel extends javax.swing.JPanel implements Savable, Refreshable {
    
    // TransformContext //////////
    protected TransformContext transformContext;
    public TransformContext getTransformContext(){ return transformContext; }

    
    public MCasingJPanel(TransformContext transformContext){
        this.transformContext = transformContext;
    }

    
    
    public abstract void doSave(Object settings, boolean validateOnly) throws Exception;
    public abstract void doRefresh(Object settings);
    
}
