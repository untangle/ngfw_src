/*
 * NetworkJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.store;

import com.metavize.gui.widgets.configWindow.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;


/**
 *
 * @author  inieves
 */
public class StoreJDialog extends ConfigJDialog {

    private StoreJPanel storeJPanel;
    private boolean purchasedTransform = false;
    private MTransformJButton mTransformJButton;
    private GridBagConstraints gridBagConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
    
    
    public StoreJDialog( MTransformJButton mTransformJButton ) {
        super(Util.getMMainJFrame());
        this.mTransformJButton = mTransformJButton;
        
        // INIT GENERAL GUI
        storeJPanel = new StoreJPanel();
        this.contentJTabbedPane.setTitleAt(0, "Procure a Software Appliance");
        this.contentJPanel.add(storeJPanel);
        this.setTitle("Procure a Software Appliance");

        storeJPanel.mTransformJPanel.add(mTransformJButton, gridBagConstraints);
        storeJPanel.descriptionJTextArea.setText(mTransformJButton.getFullDescription());
            
        this.reloadJButton.setText("<html><b>Cancel</b></html>");
        this.saveJButton.setText("<html><b>Purchase</b></html>");
        
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 480);

    }
    

    protected void doSaveJButtonActionPerformed(java.awt.event.ActionEvent evt) {
        save();
    }

    protected void doReloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {
        reload();
    }

    private void save(){
        this.purchasedTransform = true;
        
        if(Util.getIsDemo())
            this.purchasedTransform = false;
        
        this.windowClosing(null);
    }

    private void reload(){
        this.windowClosing(null);	
    }

    public MTransformJButton getPurchasedMTransformJButton(){
        if(purchasedTransform)
            return mTransformJButton;
        else
            return null;
    }
}
