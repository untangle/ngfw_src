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

package com.untangle.node.router.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.Node;
import com.untangle.node.router.*;

public class LogJPanel extends MLogTableJPanel
{
    public LogJPanel(Node node, MNodeControlsJPanel mNodeControlsJPanel)
    {
        super(node, mNodeControlsJPanel);

        final Router nat = (Router)logNode;

        setTableModel(new LogTableModel());

        EventManager<LogEvent> eventManager = nat.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        Router nat = (Router)logNode;
        EventManager<LogEvent> em = nat.getEventManager();
        EventRepository<LogEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object> {

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  55,  true,  false, false, false, String.class, null, "protocol" );
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, IPPortString.class, null, "source" );
            addTableColumn( tableColumnModel,  4,  165, true,  false, false, false, IPPortString.class, null, sc.html("original<br>destination") );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, IPPortString.class, null, sc.html("redirected<br>destination") );
            addTableColumn( tableColumnModel,  6,  150, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
            addTableColumn( tableColumnModel,  7,  100, true,  false, false, false, String.class, null, sc.html("client interface") );
            addTableColumn( tableColumnModel,  8,  105, true,  false, false, false, String.class, null, sc.html("redirected<br>server interface") );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly ) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<LogEvent> logList = (List<LogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(logList.size());
            Vector event;

            for ( LogEvent logEvent : logList ) {
                RedirectEvent log = (RedirectEvent) logEvent;
                PipelineEndpoints pe = log.getPipelineEndpoints();

                event = new Vector(8);
                event.add( null == pe ? "" : pe.getTimeStamp() );
                event.add( log.getIsDmz() ? "dmz host" : "redirect" );
                event.add( null == pe ? "" : pe.getProtocolName());
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getCServerAddr(), pe.getCServerPort()) );
                event.add( null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
                event.add( log.getIsDmz() ? "Destined to DMZ Host" : ("Redirect Rule #" + log.getRuleIndex()));
                /* This does't use the IntfEnum because there may be interfaces that
                 * are no longer avaiable.  EG. Someone installs VPN, accumulates events,
                 * and then uninstalls VPN.  The Interface will no longer be in the enum
                 * but the event will still be here. 
                 */
                String clientIntf = ( null == pe ? "" : IntfConstants.toName( pe.getClientIntf()));
                String serverIntf = ( null == pe ? "" : IntfConstants.toName( pe.getServerIntf()));
                event.add( clientIntf );
                event.add( serverIntf );
                allEvents.add( event );
            }

            return allEvents;
        }

    }
}
