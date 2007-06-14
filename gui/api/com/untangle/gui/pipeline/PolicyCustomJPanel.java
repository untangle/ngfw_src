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

import java.awt.Dialog;
import java.awt.Insets;
import java.awt.Window;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.node.firewall.*;
import com.untangle.uvm.node.firewall.intf.*;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.uvm.node.firewall.time.*;
import com.untangle.uvm.node.firewall.user.UserMatcherFactory;

public class PolicyCustomJPanel extends MEditTableJPanel {

    public PolicyCustomJPanel(MConfigJDialog mConfigJDialog) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        CustomPolicyTableModel customPolicyTableModel = new CustomPolicyTableModel(mConfigJDialog);
        //customPolicyTableModel.setOrderModelIndex(0);
        this.setTableModel( customPolicyTableModel );
        this.setAddRemoveEnabled(true);
        this.setFillJButtonEnabled(false);
    }

    protected boolean generateNewRow(int selectedModelRow){
        Vector newRow = getTableModel().generateNewRow(selectedModelRow +1);
        PolicyWizardJDialog policyWizardJDialog = new PolicyWizardJDialog((Dialog)getTopLevelAncestor(), newRow);
        policyWizardJDialog.setVisible(true);
        if(policyWizardJDialog.isProceeding()){
            getTableModel().insertNewRow(selectedModelRow, newRow);
            return true;
        }
        else{
            return false;
        }
    }

}


class CustomPolicyTableModel extends MSortedTableModel<PolicyCompoundSettings>{

    private MConfigJDialog mConfigJDialog;

    public static String TIME_EXCLUDE = "Invert day/time"; // depended on is policy wizard page
    private String TIME_INCLUDE = "Normal day/time";

    public CustomPolicyTableModel(MConfigJDialog mConfigJDialog){
        this.mConfigJDialog = mConfigJDialog;
    }

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_EDIT_MIN_WIDTH; /* # */
    private static final int C2_MW = 85;  /* edit */
    private static final int C3_MW = 55;  /* enabled */
    private static final int C4_MW = 200; /* policy / direction */
    private static final int C5_MW = 100; /* client interface */
    private static final int C6_MW = 100; /* server interface */
    private static final int C7_MW = 100; /* protocol */
    private static final int C8_MW = 100; /* client address */
    private static final int C9_MW = 100; /* server address */
    private static final int C10_MW = 100; /* client port */
    private static final int C11_MW = 100; /* server port */
    private static final int C12_MW = 165; /* user */
    private static final int C13_MW = 165; /* time range */
    private static final int C14_MW = 100; /* start time */
    private static final int C15_MW = 100; /* end time */
    private static final int C16_MW = 100; /* days */
    private static final int C17_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW + C8_MW + C9_MW + C10_MW + C11_MW + C12_MW + C13_MW + C14_MW + C15_MW + C16_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    private static final String NULL_STRING = "> No rack";
    private static final String INBOUND_STRING = " (inbound)";
    private static final String OUTBOUND_STRING = " (outbound)";


    private ComboBoxModel protocolModel =
        super.generateComboBoxModel( ProtocolMatcherFactory.getProtocolEnumeration(),
                                     ProtocolMatcherFactory.getProtocolDefault());
    private DefaultComboBoxModel policyModel = new DefaultComboBoxModel();
    private IntfEnum intfEnum;
    DefaultComboBoxModel timeModel = new DefaultComboBoxModel();
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

        timeModel.addElement(TIME_INCLUDE);
        timeModel.addElement(TIME_EXCLUDE);
        timeModel.setSelectedItem(TIME_INCLUDE);


        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min     rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW,  false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW,  false, true,  false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW,  false, true,  false, false, EditButtonRunnable.class, "true", sc.html("edit"));
        addTableColumn( tableColumnModel,  3,  C3_MW,  false, true,  false, false, Boolean.class, "true", sc.html("<b>live</b>"));
        addTableColumn( tableColumnModel,  4,  C4_MW,  true,  true,  false, false, ComboBoxModel.class, policyModel, sc.html("<b>use this rack</b> when the<br>next columns are matched..."));
        addTableColumn( tableColumnModel,  5,  C5_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("client<br>interface"));
        addTableColumn( tableColumnModel,  6,  C6_MW,  true,  true,  false, false, ComboBoxModel.class, interfaceModel, sc.html("server<br>interface"));
        addTableColumn( tableColumnModel,  7,  C7_MW,  true,  true,  false, false, ComboBoxModel.class, protocolModel, sc.html("protocol"));
        addTableColumn( tableColumnModel,  8,  C8_MW,  true,  true,  false, false, String.class, "any", sc.html("client<br>address"));
        addTableColumn( tableColumnModel,  9,  C9_MW,  true,  true,  false, false, String.class, "any", sc.html("server<br>address"));
        addTableColumn( tableColumnModel,  10, C10_MW, true,  true,  true,  false, String.class, "any", sc.html("client<br>port"));
        addTableColumn( tableColumnModel,  11, C11_MW, true,  true,  false, false, String.class, "any", sc.html("server<br>port"));
        addTableColumn( tableColumnModel,  12, C12_MW, true,  true,  false, false, UidButtonRunnable.class, "true", sc.html("user ID/login"));
        addTableColumn( tableColumnModel,  13, C13_MW, true,  true,  false, false, ComboBoxModel.class, timeModel, sc.html("day/time"));
        addTableColumn( tableColumnModel,  14, C14_MW, true,  true,  false, false, String.class, "00:00", sc.html("start time"));
        addTableColumn( tableColumnModel,  15, C15_MW, true,  true,  false, false, String.class, "23:59", sc.html("end time"));
        addTableColumn( tableColumnModel,  16, C16_MW, true,  true,  false, false, String.class, "any", sc.html("days"));
        addTableColumn( tableColumnModel,  17, C17_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  18, 18,     false, false, true,  false, UserPolicyRule.class, null, "" );

        return tableColumnModel;
    }

    protected void wireUpNewRow(Vector rowVector){
        UidButtonRunnable uidButtonRunnable = (UidButtonRunnable) rowVector.elementAt(12);
        uidButtonRunnable.setTopLevelWindow((Window) mConfigJDialog );
        EditButtonRunnable editButtonRunnable = (EditButtonRunnable) rowVector.elementAt(2);
        editButtonRunnable.setTopLevelWindow((Window) mConfigJDialog );
        editButtonRunnable.setRow(rowVector);
    }


    public void generateSettings(PolicyCompoundSettings policyCompoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
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
            newElem = (UserPolicyRule) rowVector.elementAt(18);

            boolean isLive = (Boolean) rowVector.elementAt(3);
            newElem.setLive( isLive );
            Policy policy = policyNames.get((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem());
            newElem.setPolicy( policy );
            boolean isInbound = ((String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem()).contains(INBOUND_STRING);
            newElem.setInbound( isInbound );

            newElem.setClientIntf( (IntfMatcher)((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem() );
            newElem.setServerIntf( (IntfMatcher)((ComboBoxModel)rowVector.elementAt(6)).getSelectedItem() );

            if( newElem.getClientIntf() == newElem.getServerIntf() )
                throw new Exception("In row: " + rowIndex + ". The \"client interface\" cannot match the \"server interface\"");

            try{ newElem.setProtocol( ProtocolMatcherFactory.parse(((ComboBoxModel) rowVector.elementAt(7)).getSelectedItem().toString()) ); }
            catch(Exception e){ throw new Exception("Invalid \"protocol\" in row: " + rowIndex); }
            try{ newElem.setClientAddr( ipmf.parse((String) rowVector.elementAt(8)) ); }
            catch(Exception e){ throw new Exception("Invalid \"client address\" in row: " + rowIndex); }
            try{ newElem.setServerAddr( ipmf.parse((String) rowVector.elementAt(9)) ); }
            catch(Exception e){ throw new Exception("Invalid \"server address\" in row: " + rowIndex); }
            try{ newElem.setClientPort( pmf.parse((String) rowVector.elementAt(10)) ); }
            catch(Exception e){ throw new Exception("Invalid \"client port\" in row: " + rowIndex); }
            try{ newElem.setServerPort( pmf.parse((String) rowVector.elementAt(11)) ); }
            catch(Exception e){ throw new Exception("Invalid \"server port\" in row: " + rowIndex); }
            try{ newElem.setUser( umf.parse(((UidButtonRunnable) rowVector.elementAt(12)).getUids()) ); }
            catch(Exception e){ throw new Exception("Invalid \"user name\" in row: " + rowIndex); }
            boolean invertTime = ((String)((ComboBoxModel)rowVector.elementAt(13)).getSelectedItem()).equals(TIME_EXCLUDE);
            newElem.setInvertEntireDuration(invertTime);
            try{ newElem.setStartTime( dateFormat.parse((String)rowVector.elementAt(14)) ); }
            catch(Exception e){ throw new Exception("Invalid \"start time\" in row: " + rowIndex); }
            try{ newElem.setEndTime( dateFormat.parse((String)rowVector.elementAt(15)) ); }
            catch(Exception e){ throw new Exception("Invalid \"end time\" in row: " + rowIndex); }
            if( newElem.getStartTime().compareTo(newElem.getEndTime()) > 0 )
                throw new Exception("The start time cannot be later than the end time in row: " + rowIndex);
            try{ newElem.setDayOfWeek( dmf.parse((String)rowVector.elementAt(16)) ); }
            catch(Exception e){ throw new Exception("Invalid \"days\" in row: " + rowIndex); }
            newElem.setDescription( (String) rowVector.elementAt(17) );
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
            tempRow = new Vector(19);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            EditButtonRunnable editButtonRunnable = new EditButtonRunnable("true");
            editButtonRunnable.setRow( tempRow );
            tempRow.add( editButtonRunnable );
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
            tempRow.add( super.copyComboBoxModel(timeModel) );
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
