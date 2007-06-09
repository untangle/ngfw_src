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



package com.untangle.tran.virus.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.virus.*;

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


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, sc.bold("setting value"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  false, true,  true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly){
        Vector tempRowVector;

        // ftpDisableResume
        tempRowVector = tableVector.elementAt(0);
        boolean ftpDisableResume = (Boolean) tempRowVector.elementAt(3);
        String ftpDisableResumeDetails = (String) tempRowVector.elementAt(4);

        // httpDisableResume
        tempRowVector = tableVector.elementAt(1);
        boolean httpDisableResume = (Boolean) tempRowVector.elementAt(3);
        String httpDisableResumeDetails = (String) tempRowVector.elementAt(4);

        // tricklePercent
        tempRowVector = tableVector.elementAt(2);
        int tricklePercent = (Integer) ((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue();
        String tricklePercentDetails = (String) tempRowVector.elementAt(4);

        // SAVE SETTINGS //////////
        if( !validateOnly ){
            VirusSettings virusSettings = (VirusSettings) settings;
            virusSettings.setFtpDisableResume( ftpDisableResume );
            //8/11/05 - wrs.  Commented-out as they are never "really"
            //                persisted (they are really "help") and
            //                the help text overflows the back-end DB
            //                constraint
            //      virusSettings.setFtpDisableResumeDetails( ftpDisableResumeDetails );
            virusSettings.setHttpDisableResume( httpDisableResume );
            //      virusSettings.setHttpDisableResumeDetails( httpDisableResumeDetails );
            virusSettings.setTricklePercent( tricklePercent );
            //      virusSettings.setTricklePercentDetails( tricklePercentDetails );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        VirusSettings virusSettings = (VirusSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(3);
        Vector tempRow = null;
        int rowIndex = 0;

        // ftpDisableResume
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "disable FTP download resume" );
        tempRow.add( virusSettings.getFtpDisableResume() );
        tempRow.add( "This setting specifies that if an FTP transfer has stopped or been blocked for some reason (perhaps a virus was detected), the transfer cannot be restarted from the middle where it was left off.  Allowing transfers to restart from the middle may allow unwanted traffic to enter the network." ); //virusSettings.getFtpDisableResumeDetails() );
        allRows.add( tempRow );

        // httpDisableResume
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "disable HTTP download resume" );
        tempRow.add( virusSettings.getHttpDisableResume() );
        tempRow.add( "This setting specifies that if an HTTP transfer has stopped or been blocked for some reason (perhaps a virus was detected), the transfer cannot be restarted from the middle where it was left off.  Allowing transfers to restart from the middle may allow unwanted traffic to enter the network." ); //virusSettings.getHttpDisableResumeDetails() );
        allRows.add( tempRow );

        // tricklePercent
        rowIndex++;
        tempRow = new Vector(5);
        tempRow.add( super.ROW_SAVED );
        tempRow.add( rowIndex );
        tempRow.add( "scan trickle rate (percent)" );
        tempRow.add( new SpinnerNumberModel( virusSettings.getTricklePercent(), 1, 99, 1) );
        tempRow.add( "This setting specifies the rate the user will download a file (which is being scanned), relative to the rate the Untangle Server is receiving the actual file." ); //virusSettings.getTricklePercentDetails() );
        allRows.add( tempRow );

        return allRows;
    }
}
