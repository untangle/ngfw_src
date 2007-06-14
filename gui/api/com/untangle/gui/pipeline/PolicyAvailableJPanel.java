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

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.node.*;

public class PolicyAvailableJPanel extends MEditTableJPanel {

    public PolicyAvailableJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        AvailablePolicyTableModel availablePolicyTableModel = new AvailablePolicyTableModel();
        this.setTableModel( availablePolicyTableModel );
        this.setAddRemoveEnabled(true);
        this.setFillJButtonEnabled(false);
    }
}


class AvailablePolicyTableModel extends MSortedTableModel<PolicyCompoundSettings>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class, "[no name]", sc.html("name"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, "description" );
        addTableColumn( tableColumnModel,  4, 10,    false, false, true,  false, Policy.class, null, "" );

        return tableColumnModel;
    }

    public void prevalidate(PolicyCompoundSettings policyCompoundSettings, Vector<Vector> tableVector) throws Exception {
        PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
        boolean oneRackLeft = false;
        int rowIndex = 0;
        Map<String,Object> nameMap = new HashMap<String,Object>();
        Map<String,Object> systemUsageMap = new HashMap<String,Object>();
        Map<String,Object> userUsageMap = new HashMap<String,Object>();
        // BUILD THE LIST OF SYSTEM-NEEDED POLICIES
        for( SystemPolicyRule systemPolicyRule : (List<SystemPolicyRule>) policyConfiguration.getSystemPolicyRules() )
            if( systemPolicyRule.getPolicy() != null )
                systemUsageMap.put(systemPolicyRule.getPolicy().getName(), null);
            else
                systemUsageMap.put( null, null );
        // BUILD THE LIST OF USER-NEEDED POLICIES
        for( UserPolicyRule userPolicyRule : (List<UserPolicyRule>) policyConfiguration.getUserPolicyRules() )
            if( userPolicyRule.getPolicy() != null )
                userUsageMap.put(userPolicyRule.getPolicy().getName(), null);
            else
                userUsageMap.put( null, null );

        for( Vector rowVector : tableVector ){
            rowIndex++;
            String state = (String) rowVector.elementAt(0);
            String name = (String) rowVector.elementAt(2);
            Policy policy = (Policy) rowVector.elementAt(4);

            if( ROW_REMOVE.equals(state) ){
                // if the rack is being removed, it cannot be in use by a system rule
                if( systemUsageMap.containsKey(name) ){
                    throw new Exception("The rack in row: " + rowIndex + " cannot be removed because it is currently being used in \"Default Policies\".");
                }
                // if the rack is being removed, it cannot be in use by a user rule
                else if( userUsageMap.containsKey(name) ){
                    throw new Exception("The rack in row: " + rowIndex + " cannot be removed because it is currently being used in \"Custom Policies\".");
                }
                // if the rack is being removed, it cannot be non-empty
                List<Tid> nodeInstances;
                try{
                    nodeInstances = Util.getNodeManager().nodeInstances(policy);
                }
                catch(Exception e){
                    throw new Exception("Network communication failure.  Please try again.");
                }
                if( nodeInstances.size() > 0 )
                    throw new Exception("The rack in row: " + rowIndex + " cannot be removed because it is not empty.  Please remove all products first.");
            }
            else{
                // verify there is no name collision
                if( nameMap.containsKey(name) ){
                    throw new Exception("The rack named: " + name + " in row: " + rowIndex + " already exists.");
                }
                else{
                    nameMap.put(name,null);
                }
                // racord that one rack is left
                oneRackLeft = true;
            }
        }
        // make sure there is at least one rack left
        if( !oneRackLeft ){
            throw new Exception("There must always be at least one available rack.");
        }

    }

    public void generateSettings(PolicyCompoundSettings policyCompoundSettings,
                                 Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        Policy newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (Policy) rowVector.elementAt(4);
            newElem.setName( (String) rowVector.elementAt(2) );
            newElem.setNotes( (String) rowVector.elementAt(3) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
            policyConfiguration.setPolicies( elemList );
        }
    }

    public Vector<Vector> generateRows(PolicyCompoundSettings policyCompoundSettings){
        PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
        List<Policy> policies = (List<Policy>) policyConfiguration.getPolicies();
        Vector<Vector> allRows = new Vector<Vector>(policies.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( Policy newElem : policies ){
            rowIndex++;
            tempRow = new Vector(5);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem.getNotes() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }

        return allRows;
    }
}
