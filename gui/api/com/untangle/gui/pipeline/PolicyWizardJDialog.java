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

package com.untangle.gui.pipeline;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.policy.*;
import com.untangle.gui.configuration.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class PolicyWizardJDialog extends MConfigJDialog {

    private static final String NAME_POLICY_WIZARD      = "Policy Wizard";
    
    Vector newRow;

    public PolicyWizardJDialog( Dialog parentDialog, Vector newRow ) {
        super(parentDialog);
        setTitle("Policy Manager");
        setHelpSource("policy_manager");
        this.newRow = newRow;
        saveJButton.setText("Continue");
        compoundSettings = new CompoundVector(newRow);
    }

    protected Dimension getMinSize(){
        return new Dimension(700, 480);
    }
    
    protected void generateGui(){        
        // WIZARD //////
        PolicyWizardJPanel policyWizardJPanel = new PolicyWizardJPanel(newRow);
        addScrollableTab(null, NAME_POLICY_WIZARD, null, policyWizardJPanel, false, true);
        addSavable(NAME_POLICY_WIZARD, policyWizardJPanel);
    }
    
	protected void populateAll() throws Exception {
		super.populateAll();
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
				saveJButton.setEnabled(true);
		}});
	}
	
    protected void saveAll() throws Exception{
        super.saveAll();
        isProceeding = true;
        setVisible(false);
    }

    private boolean isProceeding = false;
    public boolean isProceeding(){ return isProceeding; }

    public Vector getVector(){ return (Vector) compoundSettings; }

}

class CompoundVector implements CompoundSettings {
    private Vector vector;
    public CompoundVector(Vector v){ vector = v; }
    public Vector getVector(){ return vector; }
    public void save(){}
    public void refresh(){}
    public void validate(){}
}
