/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.pipeline;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.mvvm.policy.*;
import com.metavize.gui.configuration.*;

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
        this.newRow = newRow;
        saveJButton.setText("<html><b>Continue</b></html>");
        reloadJButton.setVisible(false);
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
    
    protected void saveAll() throws Exception{
        super.saveAll();
        setVisible(false);
        isProceeding = true;
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
