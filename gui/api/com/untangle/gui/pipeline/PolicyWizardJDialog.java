/*
 * $HeadURL:$
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

package com.untangle.gui.pipeline;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.policy.*;



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
        isProceeding = true;
        super.saveAll();
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
