/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import java.awt.Window;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.policy.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.mvvm.tran.firewall.intf.*;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.user.UserMatcherFactory;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.metavize.mvvm.tran.firewall.time.*;

public class PolicyCustomJPanel extends MEditTableJPanel {

    public PolicyCustomJPanel(MConfigJDialog mConfigJDialog) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        CustomPolicyTableModel customPolicyTableModel = new CustomPolicyTableModel(mConfigJDialog);
        this.setTableModel( customPolicyTableModel );
        this.setAddRemoveEnabled(true);
        this.setFillJButtonEnabled(false);
    }
}


class CustomPolicyTableModel extends MSortedTableModel<PolicyCompoundSettings>{
    
    private MConfigJDialog mConfigJDialog;

    public CustomPolicyTableModel(MConfigJDialog mConfigJDialog){
        this.mConfigJDialog = mConfigJDialog;
    }

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_EDIT_MIN_WIDTH; /* # */
    private static final int C2_MW = 55;  /* enabled */
    private static final int C3_MW = 200; /* policy / direction */
    private static final int C4_MW = 100; /* client interface */
    private static final int C5_MW = 100; /* server interface */
    private static final int C6_MW = 100; /* protocol */
    private static final int C7_MW = 100; /* client address */
    private static final int C8_MW = 100; /* server address */
    private static final int C9_MW = 100; /* client port */
    private static final int C10_MW = 100; /* server port */
    private static final int C11_MW = 150; /* user */
    private static final int C12_MW = 100; /* start time */
    private static final int C13_MW = 100; /* end time */
    private static final int C14_MW = 100; /* days */
    private static final int C15_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW + C11_MW + C12_MW + C13_MW + C14_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    private static final String NULL_STRING = "> No rack";
    private static final String INBOUND_STRING = " (inbound)";
    private static final String OUTBOUND_STRING = " (outbound)";


    private ComboBoxModel protocolModel = 
        super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(),
                                     ProtocolMatcherFactory.getProtocolDefault());
    private DefaultComboBoxModel policyModel = new DefaultComboBoxModel();
    private IntfEnum intfEnum;

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

        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        IntfEnum intfEnum = Util.getIntfManager().getIntfEnum();
        imf.updateEnumeration(intfEnum);
        ComboBoxModel interfaceModel = super.generateComboBoxModel( imf.getEnumeration(), imf.getDefault() );

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min     rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW,  false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW,  false, true,  false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW,  false, true,  false, false, Boolean.class, "true", sc.html("<b>live</b>"));
        addTableColumn( tableColumnModel,  3,  C3_MW,  true,  true,  false, false, ComboBoxModel.class, policyModel, sc.html("<b>use this rack</b> when the<br>next columns are matched..."));
        addTableColumn( tableColumnModel,  4,  C4_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("client<br>interface"));
        addTableColumn( tableColumnModel,  5,  C5_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("server<br>interface"));
        addTableColumn( tableColumnModel,  6,  C6_MW,  true,  true,  false, false, ComboBoxModel.class, protocolModel, sc.html("protocol"));
        addTableColumn( tableColumnModel,  7,  C7_MW,  true,  true,  false, false, String.class, "any", sc.html("client<br>address"));
        addTableColumn( tableColumnModel,  8,  C8_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>address"));
        addTableColumn( tableColumnModel,  9,  C9_MW,  true,  true,  true,  false, String.class, "any", sc.html("client<br>port"));
        addTableColumn( tableColumnModel,  10, C10_MW, true,  true,  false, false, String.class, "any", sc.html("server<br>port"));
        addTableColumn( tableColumnModel,  11, C11_MW, true,  true,  false, false, UidButtonRunnable.class, "true", sc.html("user ID/login"));
        addTableColumn( tableColumnModel,  12, C12_MW, true,  true,  false, false, String.class, "00:00", sc.html("start time"));
        addTableColumn( tableColumnModel,  13, C13_MW, true,  true,  false, false, String.class, "23:59", sc.html("end time"));
        addTableColumn( tableColumnModel,  14, C14_MW, true,  true,  false, false, String.class, "any", sc.html("days"));
        addTableColumn( tableColumnModel,  15, C15_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  16, 16,     false, false, true,  false, UserPolicyRule.class, null, "" );

        return tableColumnModel;
    }

    protected void wireUpNewRow(Vector rowVector){
        UidButtonRunnable uidButtonRunnable = (UidButtonRunnable) rowVector.elementAt(11);
        uidButtonRunnable.setTopLevelWindow((Window) mConfigJDialog );
    }


    public void generateSettings(PolicyCompoundSettings policyCompoundSettings,
				 Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        UserPolicyRule newElem = null;
        int rowIndex = 0;

        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        IPMatcherFactory ipmf  = IPMatcherFactory.getInstance();
        UserMatcherFactory umf = UserMatcherFactory.getInstance();
        DayOfWeekMatcherFactory dmf = DayOfWeekMatcherFactory.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");

	for( Vector rowVector : tableVector ){
	    rowIndex++;
        newElem = (UserPolicyRule) rowVector.elementAt(16);

        boolean isLive = (Boolean) rowVector.elementAt(2);
        newElem.setLive( isLive );
	    Policy policy = policyNames.get((String) ((ComboBoxModel)rowVector.elementAt(3)).getSelectedItem());
        newElem.setPolicy( policy );
	    boolean isInbound = ((String) ((ComboBoxModel)rowVector.elementAt(3)).getSelectedItem()).contains(INBOUND_STRING);
        newElem.setInbound( isInbound );
        
        newElem.setClientIntf( (IntfMatcher)((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
        newElem.setServerIntf( (IntfMatcher)((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem() );
            
        if( newElem.getClientIntf() == newElem.getServerIntf() )
            throw new Exception("In row: " + rowIndex + ". The \"client interface\" cannot match the \"server interface\"");
            
	    try{ newElem.setProtocol( ProtocolMatcherFactory.parse(((ComboBoxModel) rowVector.elementAt(6)).getSelectedItem().toString()) ); }
	    catch(Exception e){ throw new Exception("Invalid \"protocol\" in row: " + rowIndex); }	   
	    try{ newElem.setClientAddr( ipmf.parse((String) rowVector.elementAt(7)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client address\" in row: " + rowIndex); }
	    try{ newElem.setServerAddr( ipmf.parse((String) rowVector.elementAt(8)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server address\" in row: " + rowIndex); }
	    try{ newElem.setClientPort( pmf.parse((String) rowVector.elementAt(9)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"client port\" in row: " + rowIndex); }
	    try{ newElem.setServerPort( pmf.parse((String) rowVector.elementAt(10)) ); }
	    catch(Exception e){ throw new Exception("Invalid \"server port\" in row: " + rowIndex); }
        try{ newElem.setUser( umf.parse(((UidButtonRunnable) rowVector.elementAt(11)).getUid()) ); }
        catch(Exception e){ throw new Exception("Invalid \"user name\" in row: " + rowIndex); }
        try{ newElem.setStartTime( dateFormat.parse((String)rowVector.elementAt(12)) ); }
        catch(Exception e){ throw new Exception("Invalid \"start time\" in row: " + rowIndex); }
        try{ newElem.setEndTime( dateFormat.parse((String)rowVector.elementAt(13)) ); }
        catch(Exception e){ throw new Exception("Invalid \"end time\" in row: " + rowIndex); }
        if( newElem.getStartTime().compareTo(newElem.getEndTime()) > 0 )
            throw new Exception("The start time cannot be later than the end time in row: " + rowIndex);
        try{ newElem.setDayOfWeek( dmf.parse((String)rowVector.elementAt(14)) ); }
        catch(Exception e){ throw new Exception("Invalid \"days\" in row: " + rowIndex); }
            newElem.setDescription( (String) rowVector.elementAt(15) );
            elemList.add(newElem);
    }
    
    
    

	// SAVE SETTINGS /////////
	if( !validateOnly ){
        PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
        policyConfiguration.setUserPolicyRules( elemList );
	}
    }

    public Vector<Vector> generateRows(PolicyCompoundSettings policyCompoundSettings){

	PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
	List<UserPolicyRule> userPolicyRules = (List<UserPolicyRule>) policyConfiguration.getUserPolicyRules();
    Vector<Vector> allRows = new Vector<Vector>(userPolicyRules.size());
	Vector tempRow = null;
	int rowIndex = 0;

    IntfDBMatcher intfEnumeration[] = IntfMatcherFactory.getInstance().getEnumeration();
    
    DateFormat dateFormat = new SimpleDateFormat("HH:mm");

	updatePolicyNames( policyConfiguration.getPolicies() );
	updatePolicyModel();

	for( UserPolicyRule newElem : userPolicyRules ){
	    rowIndex++;
	    tempRow = new Vector(17);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
        tempRow.add( newElem.isLive() );
	    String policyName;
	    if( newElem.getPolicy() != null )
            policyName = newElem.getPolicy().getName() + (newElem.isInbound()?INBOUND_STRING:OUTBOUND_STRING);
	    else
            policyName = NULL_STRING;
	    tempRow.add( super.generateComboBoxModel(policyNames.keySet().toArray(), policyName) );
        tempRow.add( super.generateComboBoxModel(intfEnumeration, newElem.getClientIntf()) );
        tempRow.add( super.generateComboBoxModel(intfEnumeration, newElem.getServerIntf()) );
	    tempRow.add( super.generateComboBoxModel(ProtocolMatcherFactory.getProtocolEnumeration(), newElem.getProtocol()) );
	    tempRow.add( newElem.getClientAddr().toString() );
	    tempRow.add( newElem.getServerAddr().toString() );
	    tempRow.add( newElem.getClientPort().toString() );
	    tempRow.add( newElem.getServerPort().toString() );
        UidButtonRunnable uidButtonRunnable = new UidButtonRunnable("true");
        uidButtonRunnable.setUid( newElem.getUser().toString() );
        tempRow.add( uidButtonRunnable );
        tempRow.add( dateFormat.format(newElem.getStartTime()) );
        tempRow.add( dateFormat.format(newElem.getEndTime()) );
        tempRow.add( newElem.getDayOfWeek().toString() );
	    tempRow.add( newElem.getDescription() );
	    tempRow.add( newElem );
	    allRows.add( tempRow );
	}

        return allRows;
    }
}
