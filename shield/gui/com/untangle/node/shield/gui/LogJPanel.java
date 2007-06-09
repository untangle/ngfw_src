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

package com.untangle.tran.airgap.gui;

import java.util.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.airgap.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
        setTableModel(new LogTableModel());
        queryJComboBox.setVisible(false);
    }

    protected void refreshSettings(){
        settings = ((AirgapTransform)super.logTransform).getLogs(getEventDepth());
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null,  "timestamp" );
            addTableColumn( tableColumnModel,  1,  120, true,  false, false, false, String.class, null,  sc.html("source") );
            addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null,  sc.html("source<br>interface") );
            addTableColumn( tableColumnModel,  3,  75,  true,  false, false, false, Double.class, null,  "reputation" );
            addTableColumn( tableColumnModel,  4,  55,  true,  false, false, false, Integer.class, null, "limited" );
            addTableColumn( tableColumnModel,  5,  75,  true,  false, false, false, Integer.class, null, "dropped" );
            addTableColumn( tableColumnModel,  6,  55,  true,  false, false, false, Integer.class, null, "reject" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<ShieldRejectionLogEntry> logList = (List<ShieldRejectionLogEntry>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for ( ShieldRejectionLogEntry log : logList ) {
                event = new Vector(7);
                event.add( log.getCreateDate() );
                event.add( log.getClient() );
                event.add( log.getClientIntf() );
                event.add( log.getReputation() );
                event.add( log.getLimited() );
                event.add( log.getDropped() );
                event.add( log.getRejected() );
                allEvents.add( event );
            }
            return allEvents;
        }

    }

}
