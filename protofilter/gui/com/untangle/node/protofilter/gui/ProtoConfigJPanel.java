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

package com.untangle.node.protofilter.gui;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.protofilter.*;

public class ProtoConfigJPanel extends MEditTableJPanel{

    public ProtoConfigJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("protocols");
        super.setDetailsTitle("protocol details");
        super.setAddRemoveEnabled(true);

        // create actual table model
        ProtoTableModel protoTableModel = new ProtoTableModel();
        this.setTableModel( protoTableModel );
        protoTableModel.setSortingStatus(2, ProtoTableModel.ASCENDING);
    }
}


class ProtoTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 140; /* category */
    private static final int C3_MW = 100; /* protocol */
    private static final int C4_MW = 55;  /* block */
    private static final int C5_MW = 55;  /* log */
    private static final int C6_MW = 100; /* description */
    private static final int C7_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW), 120); /* signature */


    public TableColumnModel getTableColumnModel(){

        ProtoFilterPattern tempPattern = new ProtoFilterPattern();

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  sc.empty( "no protocol" ), "protocol");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "false", sc.bold("block"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "false", sc.bold("log"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  7, C7_MW, true,  true,  false, false, String.class,  sc.empty("no signature"), "signature");
        addTableColumn( tableColumnModel,  8, 10,    false, false, true,  false, String.class,  tempPattern.getQuality(), "");
        addTableColumn( tableColumnModel,  9, 10,    false, false, true,  false, Integer.class,  Integer.toString(tempPattern.getMetavizeId()), "");
        addTableColumn( tableColumnModel,  10, 10,    false, false, true,  false, ProtoFilterPattern.class, null, "");
        return tableColumnModel;
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
        ProtoFilterPattern newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (ProtoFilterPattern) rowVector.elementAt(10);
            newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setProtocol( (String) rowVector.elementAt(3) );
            newElem.setBlocked( (Boolean) rowVector.elementAt(4) );
            newElem.setLog( (Boolean) rowVector.elementAt(5) );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            newElem.setDefinition( (String) rowVector.elementAt(7) );
            newElem.setQuality( (String) rowVector.elementAt(8) );
            newElem.setMetavizeId( (Integer) rowVector.elementAt(9) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            ProtoFilterSettings nodeSettings = (ProtoFilterSettings) settings;
            nodeSettings.setPatterns( elemList );
        }

    }

    public Vector<Vector> generateRows(Object settings){
        ProtoFilterSettings protoFilterSettings = (ProtoFilterSettings) settings;
        List<ProtoFilterPattern> patterns = (List<ProtoFilterPattern>) protoFilterSettings.getPatterns();
        Vector<Vector> allRows = new Vector<Vector>(patterns.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( ProtoFilterPattern newElem : patterns ){
            rowIndex++;
            tempRow = new Vector(11);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getProtocol() );
            tempRow.add( newElem.isBlocked() );
            tempRow.add( newElem.getLog() );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem.getDefinition() );
            tempRow.add( newElem.getQuality() );
            tempRow.add( newElem.getMetavizeId() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
