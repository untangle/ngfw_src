/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.nat.*;

public class LogJPanel extends MLogTableJPanel
{
    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel)
    {
        super(transform, mTransformControlsJPanel);

        final Nat nat = (Nat)logTransform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<LogEvent> em = nat.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<LogEvent> eventManager = nat.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        Nat nat = (Nat)logTransform;
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
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, String.class, null, "source" );
            addTableColumn( tableColumnModel,  4,  165, true,  false, false, false, String.class, null, "original destination" );
            addTableColumn( tableColumnModel,  5,  165, true,  false, false, false, String.class, null, "redirected destination" );
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

            for (Iterator i = logList.iterator(); i.hasNext(); ) {
                RedirectEvent log = (RedirectEvent)i.next();
                PipelineEndpoints pe = log.getPipelineEndpoints();

                event = new Vector(8);
                event.add( null == pe ? "" : pe.getCreateDate() );
                event.add( log.getIsDmz() ? "dmz" : "redirect" );
                event.add( null == pe ? "" : pe.getProtocolName());
                event.add( null == pe ? "" : pe.getCClientAddr().getHostAddress() );
                event.add( null == pe ? "" : pe.getCServerAddr().getHostAddress() );
                event.add( null == pe ? "" : pe.getSServerAddr().getHostAddress() );
                event.add( log.getIsDmz() ? "Destined to DMZ" : ("Redirect Rule #" + log.getRuleIndex()));
                String clientIntf = ( null == pe ? "" : convertIntf( pe.getClientIntf()));
                String serverIntf = ( null == pe ? "" : convertIntf( pe.getServerIntf()));
                event.add( clientIntf );
                event.add( serverIntf );
                allEvents.add( event );
            }

            return allEvents;
        }

    }

    /* This function shouldn't be here */
    public String convertIntf( byte intf )
    {
        switch ( intf ) {
        case IntfConstants.EXTERNAL_INTF: return IntfConstants.EXTERNAL;
        case IntfConstants.INTERNAL_INTF: return IntfConstants.INTERNAL;
        case IntfConstants.DMZ_INTF:      return IntfConstants.DMZ;
        case IntfConstants.VPN_INTF:      return IntfConstants.VPN;
        }

        
        return "";
    }
}
