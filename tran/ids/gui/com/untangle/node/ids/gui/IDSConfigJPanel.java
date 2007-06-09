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

package com.untangle.tran.ids.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.ids.*;

public class IDSConfigJPanel extends MEditTableJPanel{

    public IDSConfigJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("protocols");
        super.setDetailsTitle("protocol details");
        super.setAddRemoveEnabled(true);

        // create actual table model
        IDSTableModel idsTableModel = new IDSTableModel();
        this.setTableModel( idsTableModel );
        idsTableModel.setSortingStatus(2, IDSTableModel.ASCENDING);
    }
}


class IDSTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 120; /* category */
    private static final int C3_MW = 55;  /* on */
    private static final int C4_MW = 55;  /* log */
    private static final int C5_MW = 180; /* description */
    private static final int C6_MW = 105; /* Show URL button */
    private static final int C7_MW = 70;  /* SID */
    private static final int C8_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW), 120); /* signature */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW,  false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW,  false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW,  true,  true,  false, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
        addTableColumn( tableColumnModel,  3,  C3_MW,  false, true,  false, false, Boolean.class, "false", sc.bold("block"));
        addTableColumn( tableColumnModel,  4,  C4_MW,  false, true,  false, false, Boolean.class, "false", sc.bold("log"));
        addTableColumn( tableColumnModel,  5,  C5_MW,  true,  false, false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  6,  C6_MW,  false,  true,  false, false, UrlButtonRunnable.class,  "false", "info URL");
        addTableColumn( tableColumnModel,  7,  C7_MW,  false, false, false, false, Integer.class, "0", "SID" );
        addTableColumn( tableColumnModel,  8,  C8_MW, true,  true,  false, false, String.class,  sc.empty("no signature"), "signature");
        addTableColumn( tableColumnModel,  9,  10,     false, false, true,  false, IDSRule.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());

        IDSRule newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (IDSRule) rowVector.elementAt(9);
            newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setLive( (Boolean) rowVector.elementAt(3) );
            newElem.setLog( (Boolean) rowVector.elementAt(4) );
            newElem.setText( (String) rowVector.elementAt(8) );

            // an optimization so that the transform knows which rows are changed
            String ruleState = (String) rowVector.elementAt(0);
            boolean ruleChanged;
            if( ROW_ADD.equals(ruleState) || ROW_CHANGED.equals(ruleState) )
                ruleChanged = true;
            else
                ruleChanged = false;
            newElem.setModified(ruleChanged);

            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            IDSSettings transformSettings = (IDSSettings) settings;
            transformSettings.setRules(elemList);
        }

    }

    public Vector<Vector> generateRows(Object settings){
        IDSSettings idsSettings = (IDSSettings) settings;
        List<IDSRule> rules = (List<IDSRule>) idsSettings.getRules();
        Vector<Vector> allRows = new Vector<Vector>(rules.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( IDSRule newElem : rules ){
            rowIndex++;
            tempRow = new Vector(10);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getLog() );
            tempRow.add( newElem.getClassification() + " (" + newElem.getDescription() + ")");
            UrlButtonRunnable urlButtonRunnable = new UrlButtonRunnable("true");
            if( (newElem.getURL() == null) || (newElem.getURL().length() == 0) )
                urlButtonRunnable.setEnabled(false);
            else
                urlButtonRunnable.setEnabled(true);
            urlButtonRunnable.setUrl( newElem.getURL() );
            tempRow.add( urlButtonRunnable );
            tempRow.add( newElem.getSid() );
            tempRow.add( newElem.getText() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
