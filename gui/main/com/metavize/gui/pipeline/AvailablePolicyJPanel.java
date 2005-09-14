/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.gui.pipeline;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.policy.*;

public class AvailablePolicyJPanel extends MEditTableJPanel {

    public AvailablePolicyJPanel() {
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


class AvailablePolicyTableModel extends MSortedTableModel{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class, "[no name]", sc.html("rack<br>name"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, "description" );
        addTableColumn( tableColumnModel,  4, 10,    false, false, true,  false, Policy.class, null, "" );

        return tableColumnModel;
    }

    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {
	PolicyConfiguration policyConfiguration = (PolicyConfiguration) settings;
	boolean oneRackLeft = false;
	int rowIndex = 0;
	Hashtable nameHashtable = new Hashtable();
	Hashtable systemUsageHashtable = new Hashtable();
	Hashtable userUsageHashtable = new Hashtable();

	List<SystemPolicyRule> systemPolicyRules = policyConfiguration.getSystemPolicyRules();
	for( SystemPolicyRule systemPolicyRule : systemPolicyRules ){
	    if( !systemUsageHashtable.containsKey(systemPolicyRule.getPolicy().getName()) )
		systemUsageHashtable.put(systemPolicyRule.getPolicy().getName(), "");
	}

	List<UserPolicyRule> userPolicyRules = policyConfiguration.getUserPolicyRules();
	for( UserPolicyRule userPolicyRule : userPolicyRules ){
	    if( !userUsageHashtable.containsKey(userPolicyRule.getPolicy().getName()) )
		userUsageHashtable.put(userPolicyRule.getPolicy().getName(), "");
	}

	for( Vector rowVector : tableVector ){
	    rowIndex++;
	    String state = (String) rowVector.elementAt(0);
	    String name = (String) rowVector.elementAt(2);

	    if( ROW_REMOVE.equals(state) ){
		// verify that if the rack is being removed, that it is not already in use by a default
		if( systemUsageHashtable.containsKey(name) ){
		    throw new Exception("The rack in row: " + rowIndex + " cannot be removed because it is currently being used in \"Default Policies\".");
		}
		// verify that if the rack is being removed, that it is not already in use by a custom rule
		else if( userUsageHashtable.containsKey(name) ){
		    throw new Exception("The rack in row: " + rowIndex + " cannot be removed because it is currently being used in \"Custom Policies\".");
		}
	    }
	    else{
		// verify there is no name collision
		if( nameHashtable.containsKey(name) ){
		    throw new Exception("The rack named: " + name + " in row: " + rowIndex + " already exists.");
		}
		else{
		    nameHashtable.put(name, name);
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

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
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
	   PolicyConfiguration policyConfiguration = (PolicyConfiguration) settings;
	   policyConfiguration.setPolicies( elemList );
	}
    }

    public Vector<Vector> generateRows(Object settings){
        PolicyConfiguration policyConfiguration = (PolicyConfiguration) settings;
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
