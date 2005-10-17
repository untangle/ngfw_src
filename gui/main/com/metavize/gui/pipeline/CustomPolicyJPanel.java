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
    private static final int C2_MW = 100; /* client interface */
    private static final int C3_MW = 100; /* server interface */
    private static final int C4_MW = 200; /* policy / direction */
    private static final int C5_MW = 100; /* protocol */
    private static final int C6_MW = 100; /* client address */
    private static final int C7_MW = 100; /* server address */
    private static final int C8_MW = 100; /* client port */
    private static final int C9_MW = 100; /* server port */
    private static final int C10_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    private static final String NULL_STRING = "> No rack";
    private static final String INBOUND_STRING = " (inbound)";
    private static final String OUTBOUND_STRING = " (outbound)";

    private IntfEnum intfEnum = Util.getNetworkingManager().getIntfEnum();

    private ComboBoxModel protocolModel = super.generateComboBoxModel( ProtocolMatcher.getProtocolEnumeration(),
								       ProtocolMatcher.getProtocolDefault() );

    private ComboBoxModel interfaceModel = super.generateComboBoxModel( intfEnum.getIntfNames(),
									intfEnum.getIntfNames()[0] );

    private DefaultComboBoxModel policyModel = new DefaultComboBoxModel();
    private Map<String,Policy> policyNames = new LinkedHashMap();
    private void updatePolicyNames(List<Policy> policyList){
	policyNames.clear();
	for( Policy policy : policyList ){
	    policyNames.put( policy.getName()+INBOUND_STRING, policy );
	    policyNames.put( policy.getName()+OUTBOUND_STRING, policy );
	}
	policyNames.put( NULL_STRING, null);
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

        addTableColumn( tableColumnModel,  2,  C2_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("client<br>interface"));
        addTableColumn( tableColumnModel,  3,  C3_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("server<br>interface"));
        addTableColumn( tableColumnModel,  4,  C4_MW,  true,  true,  false, false, ComboBoxModel.class, policyModel, sc.bold("rack"));
        addTableColumn( tableColumnModel,  5,  C5_MW,  true,  true,  false, false, ComboBoxModel.class, protocolModel, sc.html("protocol"));
        addTableColumn( tableColumnModel,  6,  C6_MW,  true,  true,  false, false, String.class, "any", sc.html("client<br>address"));
        addTableColumn( tableColumnModel,  7,  C7_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>address"));
        addTableColumn( tableColumnModel,  8,  C8_MW,  true,  true,  true,  false, String.class, "any", sc.html("client<br>port"));
        addTableColumn( tableColumnModel,  9,  C9_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>port"));
        addTableColumn( tableColumnModel,  10, C10_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  11, 10,     false, false, true,  false, UserPolicyRule.class, null, "" );

        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
	UserPolicyRule newElem = null;
	int rowIndex = 0;

	for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (UserPolicyRule) rowVector.elementAt(11);
	    newElem.setClientIntf( intfEnum.getIntfNum((String)((ComboBoxModel)rowVector.elementAt(2)).getSelectedItem()) );
	    newElem.setServerIntf( intfEnum.getIntfNum((String)((ComboBoxModel)rowVector.elementAt(3)).getSelectedItem()) );
	    if( newElem.getClientIntf() == newElem.getServerIntf() )
		throw new Exception("In row: " + rowIndex + ". The \"client interface\" cannot match the \"server interface\"");
	    Policy policy = policyNames.get((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem());
            newElem.setPolicy( policy );
	    boolean isInbound = ((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem()).contains(INBOUND_STRING);
            newElem.setInbound( isInbound );
	    try{ newElem.setProtocol( ProtocolMatcher.parse(((ComboBoxModel) rowVector.elementAt(5)).getSelectedItem().toString()) ); }
	    catch(Exception e){ throw new Exception("Invalid \"protocol\" in row: " + rowIndex); }	   
	    try{ newElem.setClientAddr( IPMatcher.parse((String) rowVector.elementAt(6)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client address\" in row: " + rowIndex); }
	    try{ newElem.setServerAddr( IPMatcher.parse((String) rowVector.elementAt(7)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server address\" in row: " + rowIndex); }
	    try{ newElem.setClientPort( PortMatcher.parse((String) rowVector.elementAt(8)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client port\" in row: " + rowIndex); }
	    try{ newElem.setServerPort( PortMatcher.parse((String) rowVector.elementAt(9)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server port\" in row: " + rowIndex); }
            newElem.setDescription( (String) rowVector.elementAt(10) );
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

	updatePolicyNames( policyConfiguration.getPolicies() );
	updatePolicyModel();

	for( UserPolicyRule newElem : userPolicyRules ){
	    rowIndex++;
	    tempRow = new Vector(13);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
	    tempRow.add( super.generateComboBoxModel(intfEnum.getIntfNames(),
						     intfEnum.getIntfName(newElem.getClientIntf())) );
	    tempRow.add( super.generateComboBoxModel(intfEnum.getIntfNames(),
						     intfEnum.getIntfName(newElem.getServerIntf())) );
	    String policyName;
	    if( newElem.getPolicy() != null )
		policyName = newElem.getPolicy().getName() + (newElem.isInbound()?INBOUND_STRING:OUTBOUND_STRING);
	    else
		policyName = NULL_STRING;
	    tempRow.add( super.generateComboBoxModel(policyNames.keySet().toArray(),
						     policyName) );
	    tempRow.add( super.generateComboBoxModel(ProtocolMatcher.getProtocolEnumeration(),
						     newElem.getProtocol()) );
	    tempRow.add( newElem.getClientAddr().toString() );
	    tempRow.add( newElem.getServerAddr().toString() );
	    tempRow.add( newElem.getClientPort().toString() );
	    tempRow.add( newElem.getServerPort().toString() );
	    tempRow.add( newElem.getDescription() );
	    tempRow.add( newElem );
	    allRows.add( tempRow );
	}

        return allRows;
    }
}
