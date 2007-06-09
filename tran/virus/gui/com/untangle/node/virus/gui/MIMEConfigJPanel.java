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
import java.util.ArrayList;
import java.util.List;
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

public class MIMEConfigJPanel extends MEditTableJPanel {

    public MIMEConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP virus scan MIME types");
        super.setDetailsTitle("MIME type description");

        // create actual table model
        MIMETableModel mimeTableModel = new MIMETableModel();
        this.setTableModel( mimeTableModel );
        this.setAddRemoveEnabled(true);
    }
}


class MIMETableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 190; /* MIME type */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */




    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "undefined type", "MIME type");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("scan"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, MimeTypeRule.class, null, "" );

        return tableColumnModel;
    }


    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List elemList = new ArrayList(tableVector.size());
        MimeTypeRule newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (MimeTypeRule) rowVector.elementAt(5);
            newElem.setMimeType( new MimeType( (String)rowVector.elementAt(2) ) ); // new because MimeType is immutable
            newElem.setLive( (Boolean) rowVector.elementAt(3)  );
            newElem.setName( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS /////////
        if( !validateOnly ){
            VirusSettings virusSettings = (VirusSettings) settings;
            virusSettings.setHttpMimeTypes( elemList );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        VirusSettings virusSettings = (VirusSettings) settings;
        List<MimeTypeRule> httpMimeTypes = (List<MimeTypeRule>) virusSettings.getHttpMimeTypes();
        Vector<Vector> allRows = new Vector<Vector>(httpMimeTypes.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( MimeTypeRule newElem : httpMimeTypes ){
            rowIndex++;
            tempRow = new Vector(6);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getMimeType().getType() );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getName() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }

        return allRows;
    }
}
