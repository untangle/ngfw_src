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

package com.untangle.tran.httpblocker.gui;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.tran.httpblocker.*;

public class BlockedCategoriesConfigJPanel extends MEditTableJPanel {


    public BlockedCategoriesConfigJPanel() {
        super(true, true);
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked Categories");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        super.setFillJButtonEnabled(false);

        // create actual table model
        CategoryTableModel categoryTableModel = new CategoryTableModel();
        super.setTableModel( categoryTableModel );
    }
}


class CategoryTableModel extends MSortedTableModel<Object> {
    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 145; /* category */
    private static final int C3_MW = 120;  /* action */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, ComboBoxModel.class, null, sc.bold("action"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  null, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, String.class,  null, "original name");
        addTableColumn( tableColumnModel,  6, 10,    false, false, true,  false, BlacklistCategory.class, null, "");
        return tableColumnModel;
    }

    private static final String ACTION_DONT_BLOCK = "do not block";
    private static final String ACTION_BLOCK = "block and log";
    private static final String ACTION_PASS  = "pass";
    private static final String ACTION_PASS_AND_LOG = "pass and log";


    private DefaultComboBoxModel categoryComboBoxModel;
    private DefaultComboBoxModel fascistComboBoxModel;


    public CategoryTableModel(){
        categoryComboBoxModel = new DefaultComboBoxModel();
        categoryComboBoxModel.addElement( ACTION_BLOCK );
        categoryComboBoxModel.addElement( ACTION_PASS );
        categoryComboBoxModel.addElement( ACTION_PASS_AND_LOG );
        fascistComboBoxModel = new DefaultComboBoxModel();
        fascistComboBoxModel.addElement( ACTION_DONT_BLOCK );
        fascistComboBoxModel.addElement( ACTION_BLOCK );
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        BlacklistCategory newElem = null;

        int i = 0;
        boolean isFascistMode = false;
        for( Vector rowVector : tableVector ){
            if( 0 == i ){ // HACK TO DEAL WITH FASCIST MODE ROW
                String selectedItem = (String) ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem();
                isFascistMode = selectedItem.equals(ACTION_BLOCK);
            }
            else{
                newElem = (BlacklistCategory) rowVector.elementAt(6);
                newElem.setDisplayName( (String) rowVector.elementAt(2) );
                String selectedItem = (String) ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem();
                if( selectedItem.equals(ACTION_BLOCK)  ){
                    newElem.setBlockDomains( true );
                    newElem.setBlockUrls( true );
                    newElem.setBlockExpressions( true );
                    newElem.setLogOnly(false); // this setting wont/shouldnt actually be used since block implies log
                }
                else if( selectedItem.equals(ACTION_PASS)  ){
                    newElem.setBlockDomains( false );
                    newElem.setBlockUrls( false );
                    newElem.setBlockExpressions( false );
                    newElem.setLogOnly(false);
                }
                else{ // ACTION_PASS_AND_LOG
                    newElem.setBlockDomains( true );
                    newElem.setBlockUrls( true );
                    newElem.setBlockExpressions( true );
                    newElem.setLogOnly(true);
                }
                newElem.setDescription( (String) rowVector.elementAt(4) );
                newElem.setName( (String) rowVector.elementAt(5) );
                elemList.add(newElem);
            }
            i++;
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
            httpBlockerSettings.setBlacklistCategories( elemList );
            httpBlockerSettings.setFascistMode( isFascistMode );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        HttpBlockerSettings httpBlockerSettings = (HttpBlockerSettings) settings;
        List<BlacklistCategory> blacklistCategories = (List<BlacklistCategory>) httpBlockerSettings.getBlacklistCategories();
        Vector<Vector> allRows = new Vector<Vector>(blacklistCategories.size());
        Vector tempRow = null;
        int rowIndex = 0;

        // HACK TO DEAL WITH FASCIST MODE
        tempRow = new Vector(7);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "All Web Content" );

        ComboBoxModel newComboBoxModel = copyComboBoxModel(fascistComboBoxModel);
        if( httpBlockerSettings.getFascistMode() ){
            newComboBoxModel.setSelectedItem( ACTION_BLOCK );
        }
        else{
            newComboBoxModel.setSelectedItem( ACTION_DONT_BLOCK );
        }
        tempRow.add( newComboBoxModel );
        tempRow.add( "Blocks any web page that is not in the pass lists." );
        tempRow.add( "" );
        tempRow.add( null );
        allRows.add( tempRow );

        // DEAL WITH ALL OTHER ROWS
        for( BlacklistCategory newElem : blacklistCategories ){
            rowIndex++;
            tempRow = new Vector(7);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getDisplayName() );

            ComboBoxModel comboBoxModel = copyComboBoxModel(categoryComboBoxModel);

            if( newElem.getLogOnly() ) {
                comboBoxModel.setSelectedItem( ACTION_PASS_AND_LOG );
            }
            else if( newElem.getBlockDomains() ) {
                comboBoxModel.setSelectedItem( ACTION_BLOCK );
            }
            else{
                comboBoxModel.setSelectedItem( ACTION_PASS );
            }
            tempRow.add( comboBoxModel );

            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }

        return allRows;
    }
}
