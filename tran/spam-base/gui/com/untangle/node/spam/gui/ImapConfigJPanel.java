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


package com.untangle.node.spam.gui;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.spam.*;


public class ImapConfigJPanel extends MEditTableJPanel {

    public ImapConfigJPanel() {

        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spam filter rules");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);

        // create actual table model
        ImapTableModel imapTableModel = new ImapTableModel();
        this.setTableModel( imapTableModel );
    }
}


class ImapTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 125; /* source */
    private static final int C3_MW = 55;  /* scan */
    private static final int C4_MW = 95; /* scan strength */
    private static final int C5_MW = 125; /* action if SPAM detected */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    protected boolean getSortable(){ return false; }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, false, false, false, false, String.class,  null, "source");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class,  null, sc.bold("scan") );
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("scan<br>strength"));
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, ComboBoxModel.class,  null, sc.html("action if<br>SPAM detected"));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, SpamIMAPConfig.class,  null, "");
        return tableColumnModel;
    }

    private static final String SOURCE_INBOUND  = "incoming message";
    private static final String SOURCE_OUTBOUND = "outgoing message";

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        SpamIMAPConfig spamIMAPConfigInbound = null;
        SpamIMAPConfig spamIMAPConfigOutbound = null;

        for( Vector rowVector : tableVector ){
            SpamIMAPConfig spamIMAPConfig = (SpamIMAPConfig) rowVector.elementAt(7);
            spamIMAPConfig.setScan( (Boolean) rowVector.elementAt(3) );
            spamIMAPConfig.setStrengthByName( (String) ((ComboBoxModel)rowVector.elementAt(4)).getSelectedItem() );
            spamIMAPConfig.setMsgAction( (SpamMessageAction) ((ComboBoxModel)rowVector.elementAt(5)).getSelectedItem() );
            spamIMAPConfig.setNotes( (String) rowVector.elementAt(6) );

            if( ((String)rowVector.elementAt(2)).equals(SOURCE_INBOUND) ){
                spamIMAPConfigInbound = spamIMAPConfig;
            }
            else if( ((String)rowVector.elementAt(2)).equals(SOURCE_OUTBOUND) ){
                spamIMAPConfigOutbound = spamIMAPConfig;
            }
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            SpamSettings spamSettings = (SpamSettings) settings;
            spamSettings.setIMAPInbound( spamIMAPConfigInbound );
            spamSettings.setIMAPOutbound( spamIMAPConfigOutbound );
        }

    }

    public Vector<Vector> generateRows(Object settings) {
        SpamSettings spamSettings = (SpamSettings) settings;
        Vector<Vector> allRows = new Vector<Vector>(2);
        int rowIndex = 0;

        // INBOUND
        rowIndex++;
        Vector inboundRow = new Vector(8);
        SpamIMAPConfig spamIMAPConfigInbound = spamSettings.getIMAPInbound();
        inboundRow.add( super.ROW_SAVED );
        inboundRow.add( rowIndex );
        inboundRow.add( SOURCE_INBOUND );
        inboundRow.add( spamIMAPConfigInbound.getScan() );
        inboundRow.add( super.generateComboBoxModel(SpamIMAPConfig.getScanStrengthEnumeration(), spamIMAPConfigInbound.getStrengthByName()) );
        inboundRow.add( super.generateComboBoxModel(SpamMessageAction.getValues(), spamIMAPConfigInbound.getMsgAction()) );
        inboundRow.add( spamIMAPConfigInbound.getNotes() );
        inboundRow.add( spamIMAPConfigInbound );
        allRows.add(inboundRow);

        // OUTBOUND
        rowIndex++;
        Vector outboundRow = new Vector(8);
        SpamIMAPConfig spamIMAPConfigOutbound = spamSettings.getIMAPOutbound();
        outboundRow.add( super.ROW_SAVED );
        outboundRow.add( rowIndex );
        outboundRow.add( SOURCE_OUTBOUND );
        outboundRow.add( spamIMAPConfigOutbound.getScan() );
        outboundRow.add( super.generateComboBoxModel(SpamIMAPConfig.getScanStrengthEnumeration(), spamIMAPConfigOutbound.getStrengthByName()) );
        outboundRow.add( super.generateComboBoxModel(SpamMessageAction.getValues(), spamIMAPConfigOutbound.getMsgAction()) );
        outboundRow.add( spamIMAPConfigOutbound.getNotes() );
        outboundRow.add( spamIMAPConfigOutbound );
        allRows.add(outboundRow);

        return allRows;
    }
}
