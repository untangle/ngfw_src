/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.protofilter.gui;

import java.util.*;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.node.protofilter.*;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.PipelineEndpoints;

public class LogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Protocol blocked events";

    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel){
        super(node, mNodeControlsJPanel);

        final ProtoFilter protoFilter = (ProtoFilter)logNode;

        setTableModel(new LogTableModel());

        EventManager<ProtoFilterLogEvent> eventManager = protoFilter.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        ProtoFilter protoFilter = (ProtoFilter)logNode;
        EventManager<ProtoFilterLogEvent> em = protoFilter.getEventManager();
        EventRepository<ProtoFilterLogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }


    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, IPPortString.class, null, "client" );
            addTableColumn( tableColumnModel,  3,  100, true,  false, false, true,  String.class, null, "request" );
            addTableColumn( tableColumnModel,  4,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, IPPortString.class, null, "server" );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<ProtoFilterLogEvent> logList = (List<ProtoFilterLogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for( ProtoFilterLogEvent log : logList ){
                PipelineEndpoints pe = log.getPipelineEndpoints();
                event = new Vector(7);
                event.add( log.getTimeStamp() );
                event.add( log.isBlocked() ? "blocked" : "passed" );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( log.getProtocol() );
                event.add( log.isBlocked() ? "blocked in block list" : "not blocked in block list");
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

}
