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

package com.untangle.node.ips.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.Node;
import com.untangle.node.ips.*;

public class LogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Packet blocked events";

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final IPSNode ips = (IPSNode)node;

        setTableModel(new LogTableModel());

        EventManager<IPSLogEvent> eventManager = ips.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        IPSNode ips = (IPSNode)logNode;
        EventManager<IPSLogEvent> em = ips.getEventManager();
        EventRepository<IPSLogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, sc.html("client") );
            addTableColumn( tableColumnModel,  3,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("direction") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, IPPortString.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}


        public Vector<Vector> generateRows(Object settings){
            List<IPSLogEvent> logList = (List<IPSLogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for( IPSLogEvent log : logList ){
                PipelineEndpoints pe = log.getPipelineEndpoints();

                event = new Vector(6);
                event.add( log.getTimeStamp() );
                event.add( log.isBlocked() ? "block" : "pass" );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( "#" + log.getRuleSid() + ": " + log.getMessage() );
                event.add( null == pe ? "" : pe.getDirectionName() );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }
    }
}
