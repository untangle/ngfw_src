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

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.policy.*;
import com.untangle.mvvm.tran.*;

public class PolicyDefaultJPanel extends MEditTableJPanel {

    public PolicyDefaultJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");

        // create actual table model
        DefaultPolicyTableModel defaultPolicyTableModel = new DefaultPolicyTableModel();
        this.setTableModel( defaultPolicyTableModel );
        this.setAddRemoveEnabled(false);
        this.setFillJButtonEnabled(false);
    }
}


class DefaultPolicyTableModel extends MSortedTableModel<PolicyCompoundSettings>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* invisible */
    private static final int C2_MW = 200; /* policy / direction */
    private static final int C3_MW = 100; /* client interface */
    private static final int C4_MW = 100; /* server interface */
    private static final int C5_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW), 125); /* description */

    protected boolean getSortable(){ return false; }

    private static final String NULL_STRING = "> No rack";
    private static final String INBOUND_STRING = " (inbound)";
    private static final String OUTBOUND_STRING = " (outbound)";
    private Map<String,Policy> policyNames = new LinkedHashMap();
    private void updatePolicyNames(List<Policy> policyList){
        policyNames.clear();
        for( Policy policy : policyList ){
            policyNames.put( policy.getName() + INBOUND_STRING, policy );
            policyNames.put( policy.getName() + OUTBOUND_STRING, policy );
        }
        policyNames.put( NULL_STRING, null );
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, ComboBoxModel.class, null, sc.html("<b>use this rack</b> when the<br>next columns are matched..."));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  false, false, false, String.class, null, sc.html("client<br>interface"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, false, false, String.class, null, sc.html("server<br>interface"));
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  false, true,  String.class, null, "description" );
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, SystemPolicyRule.class, null, "" );

        return tableColumnModel;
    }

    public void generateSettings(PolicyCompoundSettings policyCompoundSettings,
                                 Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        SystemPolicyRule newElem = null;

        int rowIndex = 0;
        int parity = 0;
        Policy firstPolicy = null;
        boolean firstInbound = false;

        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (SystemPolicyRule) rowVector.elementAt(6);
            Policy policy = policyNames.get((String) ((ComboBoxModel)rowVector.elementAt(2)).getSelectedItem());
            newElem.setPolicy( policy );
            boolean isInbound = ((String) ((ComboBoxModel)rowVector.elementAt(2)).getSelectedItem()).contains(INBOUND_STRING);
            newElem.setInbound( isInbound );
            newElem.setDescription( (String) rowVector.elementAt(5) );
            elemList.add(newElem);
            if( parity == 0 ){
                firstPolicy = policy;
                firstInbound = isInbound;
            }
            else{
                if( nullOk(policy,firstPolicy) ){
                    // ok
                }
                else{
                    if( policy.equals(firstPolicy) ){
                        if( isInbound != firstInbound ){
                            // ok
                        }
                        else{
                            throw new Exception("The racks chosen in rows "
                                                + (rowIndex-1)
                                                + " and "
                                                + rowIndex
                                                + " must be in opposite directions.");
                        }
                    }
                    else{
                        if( isInbound != firstInbound ){
                            throw new Exception("The racks chosen in rows "
                                                + (rowIndex-1)
                                                + " and "
                                                + rowIndex
                                                + " must be the same rack.");
                        }
                        else{
                            throw new Exception("The racks chosen in rows "
                                                + (rowIndex-1)
                                                + " and "
                                                + rowIndex
                                                + " must be the same rack, and in opposite directions.");
                        }
                    }
                }
            }
            parity = (parity+1)%2;
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
            policyConfiguration.setSystemPolicyRules( elemList );
        }

    }

    public Vector<Vector> generateRows(PolicyCompoundSettings policyCompoundSettings){
        PolicyConfiguration policyConfiguration = policyCompoundSettings.getPolicyConfiguration();
        List<SystemPolicyRule> systemPolicyRules = (List<SystemPolicyRule>) policyConfiguration.getSystemPolicyRules();
        Vector<Vector> allRows = new Vector<Vector>(systemPolicyRules.size());
        Vector tempRow = null;
        int rowIndex = 0;

        updatePolicyNames( policyConfiguration.getPolicies() );

        for( SystemPolicyRule newElem : systemPolicyRules ){
            rowIndex++;
            tempRow = new Vector(7);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            String policyName;
            if( newElem.getPolicy() != null )
                policyName = newElem.getPolicy().getName() + (newElem.isInbound()?INBOUND_STRING:OUTBOUND_STRING);
            else
                policyName = NULL_STRING;
            tempRow.add( super.generateComboBoxModel(policyNames.keySet().toArray(),
                                                     policyName) );
            tempRow.add( policyCompoundSettings.getIntfEnum().getIntfName( newElem.getClientIntf() ) );
            tempRow.add( policyCompoundSettings.getIntfEnum().getIntfName( newElem.getServerIntf() ) );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }

        return allRows;
    }

    private boolean nullOk(Object o1, Object o2){
        if( (o1==null) && (o2==null) )
            return true;
        else if( (o1==null) ^ (o2==null) )
            return true;
        else
            return false;
    }

}
