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

package com.untangle.tran.portal.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.portal.*;
import com.untangle.mvvm.portal.*;

public class LogJPanel extends MLogTableJPanel {


    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final PortalTransform portalTransform = (PortalTransform)transform;

        setTableModel(new LogTableModel());
        queryJComboBox.setVisible(false); // since only one item right now
        EventManager<PortalEvent> eventManager = portalTransform.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }
    }

    protected void refreshSettings(){
        PortalTransform portalTransform = (PortalTransform)logTransform;
        EventManager<PortalEvent> em = portalTransform.getEventManager();
        EventRepository<PortalEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }

    class LogTableModel extends MSortedTableModel<Object>{

        public TableColumnModel getTableColumnModel(){
            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min  rsz    edit   remv   desc   typ               def
            addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
            addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
            addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "user id/login" );
            addTableColumn( tableColumnModel,  3,  165, true,  false, false, false, IPaddrString.class, null, "client" );
            addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("success") );
            addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("reason") );
            return tableColumnModel;
        }

        public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}

        public Vector<Vector> generateRows(Object settings){
            List<LogEvent> requestLogList = (List<LogEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( LogEvent requestLog : requestLogList ){
                event = new Vector(6);

                event.add( requestLog.getTimeStamp() );

		if( requestLog instanceof PortalLogoutEvent ){
		    PortalLogoutEvent newEvent = (PortalLogoutEvent) requestLog;
		    event.add( "Logout" );
		    event.add( newEvent.getUid() );
		    event.add( new IPaddrString(newEvent.getClientAddr()) );
		    event.add( "Success" );
                    String reason = "";
                    if (newEvent.getReason() != null)
                        reason = newEvent.getReason().toString();
		    event.add( reason );
		    allEvents.add( event );
		}
		else if( requestLog instanceof PortalLoginEvent ){
		    PortalLoginEvent newEvent = (PortalLoginEvent) requestLog;
		    event.add( "Login" );
		    event.add( newEvent.getUid() );
		    event.add( new IPaddrString(newEvent.getClientAddr()) );
		    event.add( newEvent.isSucceeded()==true?"Success":"Failure" );
                    String reason = "";
                    if (!newEvent.isSucceeded())
                        if (newEvent.getReason() != null)
                            reason = newEvent.getReason().toString();
		    event.add( reason );
		    allEvents.add( event );
		}
            }

            return allEvents;
        }

    }

}
