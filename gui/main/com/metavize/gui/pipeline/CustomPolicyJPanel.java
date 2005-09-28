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
import com.metavize.mvvm.tran.firewall.*;

public class CustomPolicyJPanel extends MEditTableJPanel {

    public CustomPolicyJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        CustomPolicyTableModel customPolicyTableModel = new CustomPolicyTableModel();
        this.setTableModel( customPolicyTableModel );
        this.setAddRemoveEnabled(true);
	this.setFillJButtonEnabled(false);
    }
}


class CustomPolicyTableModel extends MSortedTableModel{
    
    public CustomPolicyTableModel(){

    }

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 100; /* protocol */
    private static final int C3_MW = 100; /* client interface */
    private static final int C4_MW = 100; /* server interface */
    private static final int C5_MW = 100; /* client address */
    private static final int C6_MW = 100; /* server address */
    private static final int C7_MW = 100; /* client port */
    private static final int C8_MW = 100; /* server port */
    private static final int C9_MW = 100; /* direction */
    private static final int C10_MW = 100; /* policy */
    private static final int C11_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    private static final String DIRECTION_INBOUND = "inbound";
    private static final String DIRECTION_OUTBOUND = "outbound";

    private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(),
								       ProtocolMatcher.getProtocolDefault() );
    private ComboBoxModel directionModel = super.generateComboBoxModel(new String[]{DIRECTION_INBOUND, DIRECTION_OUTBOUND},
								       DIRECTION_INBOUND);

    private DefaultComboBoxModel policyModel = new DefaultComboBoxModel();
    private Map<String,Policy> policyNames = new LinkedHashMap();
    private void updatePolicyNames(){
	policyNames.clear();
	List<Policy> policyList = Util.getPolicyManager().getPolicyConfiguration().getPolicies();
	for( Policy policy : policyList ){
	    policyNames.put( policy.getName(), policy );
	}
    }
    private void updatePolicyModel(){
	policyModel.removeAllElements();
	for( String name : policyNames.keySet() ){
	    policyModel.addElement(name);
	}
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min     rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW,  false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW,  false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW,  true,  true,  false, false, ComboBoxModel.class, protocolModel, sc.html("protocol"));
        addTableColumn( tableColumnModel,  3,  C3_MW,  true,  true,  false, false, String.class, "0", sc.html("client<br>interface"));
        addTableColumn( tableColumnModel,  4,  C4_MW,  true,  true,  false, false, String.class, "1", sc.html("server<br>interface"));
        addTableColumn( tableColumnModel,  5,  C5_MW,  true,  true,  false, false, String.class, "any", sc.html("client<br>address"));
        addTableColumn( tableColumnModel,  6,  C6_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>address"));
        addTableColumn( tableColumnModel,  7,  C7_MW,  true,  true,  false, false, String.class, "any", sc.html("client<br>port"));
        addTableColumn( tableColumnModel,  8,  C8_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>port"));
        addTableColumn( tableColumnModel,  9,  C9_MW,  true,  true,  false, false, ComboBoxModel.class, directionModel, sc.html("direction"));
        addTableColumn( tableColumnModel,  10, C10_MW, true,  true,  false, false, ComboBoxModel.class, policyModel, sc.bold("rack"));
        addTableColumn( tableColumnModel,  11, C11_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  12, 10,     false, false, true,  false, UserPolicyRule.class, null, "" );

        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
	UserPolicyRule newElem = null;
	int rowIndex = 0;

	for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (UserPolicyRule) rowVector.elementAt(12);
	    try{ newElem.setProtocol( ProtocolMatcher.parse(((ComboBoxModel) rowVector.elementAt(2)).getSelectedItem().toString()) ); }
	    catch(Exception e){ throw new Exception("Invalid \"protocol\" in row: " + rowIndex); }
	    try{ newElem.setClientIntf( Byte.decode((String) rowVector.elementAt(3)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client interface\" in row: " + rowIndex); }
	    try{ newElem.setServerIntf( Byte.decode((String) rowVector.elementAt(4)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server interface\" in row: " + rowIndex); }
	    if( (new Byte(newElem.getClientIntf())).equals(new Byte(newElem.getServerIntf())) )
		throw new Exception("In row: " + rowIndex + ". The \"client address\" cannot match the \"server address\"");
	    try{ newElem.setClientAddr( IPMatcher.parse((String) rowVector.elementAt(5)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client address\" in row: " + rowIndex); }
	    try{ newElem.setServerAddr( IPMatcher.parse((String) rowVector.elementAt(6)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server address\" in row: " + rowIndex); }
	    try{ newElem.setClientPort( PortMatcher.parse((String) rowVector.elementAt(7)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client port\" in row: " + rowIndex); }
	    try{ newElem.setServerPort( PortMatcher.parse((String) rowVector.elementAt(8)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server port\" in row: " + rowIndex); }
            newElem.setInbound( ((ComboBoxModel)rowVector.elementAt(9)).getSelectedItem().equals(DIRECTION_INBOUND) );
            newElem.setPolicy( policyNames.get((String) ((ComboBoxModel)rowVector.elementAt(10)).getSelectedItem()) );
            newElem.setDescription( (String) rowVector.elementAt(11) );
            elemList.add(newElem);
        }

	// SAVE SETTINGS /////////
	if( !validateOnly ){
	   PolicyConfiguration policyConfiguration = (PolicyConfiguration) settings;
	   policyConfiguration.setUserPolicyRules( elemList );
	}
    }

    public Vector<Vector> generateRows(Object settings){
        PolicyConfiguration policyConfiguration = (PolicyConfiguration) settings;
	List<UserPolicyRule> userPolicyRules = (List<UserPolicyRule>) policyConfiguration.getUserPolicyRules();
        Vector<Vector> allRows = new Vector<Vector>(userPolicyRules.size());
	Vector tempRow = null;
	int rowIndex = 0;

	updatePolicyNames();
	updatePolicyModel();

	for( UserPolicyRule newElem : userPolicyRules ){
	    rowIndex++;
	    tempRow = new Vector(13);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
	    tempRow.add( super.copyComboBoxModel(protocolModel) );
	    tempRow.add( ((Byte)newElem.getClientIntf()).toString() );
	    tempRow.add( ((Byte)newElem.getServerIntf()).toString() );
	    tempRow.add( newElem.getClientAddr().toString() );
	    tempRow.add( newElem.getServerAddr().toString() );
	    tempRow.add( newElem.getClientPort().toString() );
	    tempRow.add( newElem.getServerPort().toString() );
	    ComboBoxModel directionComboBoxModel = super.copyComboBoxModel(directionModel);
	    if( newElem.isInbound() )
		directionComboBoxModel.setSelectedItem(DIRECTION_INBOUND);
	    else
		directionComboBoxModel.setSelectedItem(DIRECTION_OUTBOUND);
	    tempRow.add( directionComboBoxModel );
	    tempRow.add( super.generateComboBoxModel(policyNames.keySet().toArray(), newElem.getPolicy().getName()) );
	    tempRow.add( newElem.getDescription() );
	    tempRow.add( newElem );
	    allRows.add( tempRow );
	}

        return allRows;
    }
}
