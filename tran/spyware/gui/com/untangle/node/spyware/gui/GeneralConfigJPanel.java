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



package com.untangle.tran.spyware.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.tran.http.UserWhitelistMode;
import com.untangle.tran.spyware.*;


public class GeneralConfigJPanel extends MEditTableJPanel {


    public GeneralConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel();
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, true,  true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        Vector tempRowVector;

        // blockAllActiveX
        tempRowVector = tableVector.elementAt(0);
        boolean blockAllActiveX = (Boolean) tempRowVector.elementAt(3);
        String blockAllActiveXDetails = (String) tempRowVector.elementAt(4);

        // user white list mode
        tempRowVector = tableVector.elementAt(1);
        UserWhitelistMode userWhitelistMode = (UserWhitelistMode) ((ComboBoxModel)tempRowVector.elementAt(3)).getSelectedItem();


        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            SpywareSettings spywareSettings = (SpywareSettings) settings;
            spywareSettings.setBlockAllActiveX( blockAllActiveX );
            spywareSettings.setBlockAllActiveXDetails( blockAllActiveXDetails );
            spywareSettings.setUserWhitelistMode( userWhitelistMode );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        SpywareSettings spywareSettings = (SpywareSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(1);
        Vector tempRow = null;
        int rowIndex = 0;

        // blockAllActiveX
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "block all ActiveX" );
        tempRow.add( spywareSettings.getBlockAllActiveX() );
        tempRow.add( "This settings allows you to block ActiveX from being transferred, regardless of if the ActiveX is known to the ActiveX Block List or not." );//spywareSettings.getBlockAllActiveXDetails());
        allRows.add( tempRow );

        // user white list
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "quick-passlist" );
        DefaultComboBoxModel whitelistComboBoxModel = new DefaultComboBoxModel();
        whitelistComboBoxModel.addElement(UserWhitelistMode.NONE);
        whitelistComboBoxModel.addElement(UserWhitelistMode.USER_ONLY);
        whitelistComboBoxModel.addElement(UserWhitelistMode.USER_AND_GLOBAL);
        whitelistComboBoxModel.setSelectedItem(spywareSettings.getUserWhitelistMode());
        tempRow.add( whitelistComboBoxModel );
        tempRow.add( "This setting allows a user to unblock a webpage that was blocked by Spyware Blocker.  The blocked webpage will have buttons that can be clicked." );
        allRows.add( tempRow );

        return allRows;
    }
}
