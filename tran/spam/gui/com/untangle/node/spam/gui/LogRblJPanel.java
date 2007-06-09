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

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.Node;
import com.untangle.node.spam.*;

public class LogRblJPanel extends MLogTableJPanel {

    //    private static final String SPAM_EVENTS_STRING = "Spam detected events";

    public LogRblJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final SpamNode spam = (SpamNode)logNode;

        setTableModel(new LogTableModel());

        EventManager<SpamSMTPRBLEvent> eventManager = spam.getRBLEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        SpamNode spam = (SpamNode)logNode;
        EventManager<SpamSMTPRBLEvent> em = spam.getRBLEventManager();
        EventRepository<SpamSMTPRBLEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ           def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,   90, true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPaddrString.class, null, "sender" );
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, String.class, null, "dnsbl server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<SpamSMTPRBLEvent> requestLogList = (List<SpamSMTPRBLEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( SpamSMTPRBLEvent requestLog : requestLogList ){
                event = new Vector(4);
                event.add( requestLog.getTimeStamp() );
                event.add( (requestLog.getSkipped()?"skipped":"blocked") );
                event.add( new IPaddrString( requestLog.getIPAddr()) );
                event.add( requestLog.getHostname().toString() );
                allEvents.add( event );
            }

            return allEvents;
        }
    }

}
