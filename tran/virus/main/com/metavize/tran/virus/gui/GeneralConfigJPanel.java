/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.virus.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {

    public GeneralConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel(transformContext);
        this.setTableModel( tableModel );
    }
}


class GeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C1_MW + C2_MW + C3_MW), 120); /* description */
    private static final StringConstants sc = StringConstants.getInstance();

    GeneralTableModel(TransformContext transformContext){
        super(transformContext);
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, "setting value");
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class,
                        sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        return tableColumnModel;
    }

    public Object generateSettings(Vector dataVector){
        Vector tempRowVector;
        
        VirusSettings virusSettings = ((VirusTransform)transformContext.transform()).getVirusSettings();
        VirusConfig virusInboundFtpCtl   = virusSettings.getFtpInbound();
	VirusConfig virusOutboundFtpCtl  = virusSettings.getFtpOutbound();
	VirusConfig virusInboundHttpCtl  = virusSettings.getHttpInbound();
	VirusConfig virusOutboundHttpCtl = virusSettings.getHttpOutbound();


        // ftpDisableResume
        tempRowVector = (Vector) dataVector.elementAt(0);
        virusSettings.setFtpDisableResume( (Boolean) tempRowVector.elementAt(3) );
        virusSettings.setFtpDisableResumeDetails( (String) tempRowVector.elementAt(4) );
        
        // httpDisableResume
        tempRowVector = (Vector) dataVector.elementAt(1);
        virusSettings.setHttpDisableResume( (Boolean) tempRowVector.elementAt(3) );
        virusSettings.setHttpDisableResumeDetails( (String) tempRowVector.elementAt(4) );
        
        // tricklePercent
        tempRowVector = (Vector) dataVector.elementAt(2);
        virusSettings.setTricklePercent( ((Integer)((SpinnerNumberModel)tempRowVector.elementAt(3)).getValue()).intValue() );
        virusSettings.setTricklePercentDetails( (String) tempRowVector.elementAt(4) );

	/*        
	// keep copy of inbound infected ftp files
	tempRowVector = (Vector) dataVector.elementAt(3);
	virusInboundFtpCtl.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusInboundFtpCtl.setCopyOnBlockNotes( (String) tempRowVector.elementAt(4) );

	// keep copy of outbound infected ftp files
	tempRowVector = (Vector) dataVector.elementAt(4);
	virusOutboundFtpCtl.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusOutboundFtpCtl.setCopyOnBlockNotes( (String) tempRowVector.elementAt(4) );

	// keep copy of inbound infected http files
	tempRowVector = (Vector) dataVector.elementAt(5);
	virusInboundHttpCtl.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusInboundHttpCtl.setCopyOnBlockNotes( (String) tempRowVector.elementAt(4) );

	// keep copy of outbound infected http files
	tempRowVector = (Vector) dataVector.elementAt(6);
	virusOutboundHttpCtl.setCopyOnBlock( (Boolean) tempRowVector.elementAt(3) );
	virusOutboundHttpCtl.setCopyOnBlockNotes( (String) tempRowVector.elementAt(4) );
	*/

        return virusSettings;
    }
    
    public Vector generateRows(Object transformSettings){
	VirusSettings virusSettings = (VirusSettings) transformSettings;
        Vector allRows = new Vector(8);
        Vector tempRowVector;

        // ftpDisableResume
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(1));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("disable FTP download resuming, so downloads cannot be restarted in the middle after being stopped");
        tempRowVector.add( virusSettings.getFtpDisableResume() );
        tempRowVector.add( virusSettings.getFtpDisableResumeDetails() );
        allRows.add( tempRowVector );
        
        // httpDisableResume
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(2));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("disable HTTP download resume, so downloads cannot be restarted in the middle after being stopped");
        tempRowVector.add( virusSettings.getHttpDisableResume() );
        tempRowVector.add( virusSettings.getHttpDisableResumeDetails() );
        allRows.add( tempRowVector );
        
        // tricklePercent
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(3));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("trickle rate during scan (percent), which is the rate the user will download a file being scanned, relative to the rate the EdgeGuard is receiving the actual file");
        tempRowVector.add( new SpinnerNumberModel( virusSettings.getTricklePercent(), 1, 99, 1) );
        tempRowVector.add( virusSettings.getTricklePercentDetails() );
        allRows.add( tempRowVector );

	/*
        // keep copy of inbound infected ftp files
	tempRowVector = new Vector(4);
	tempRowVector.add(new Integer(4));
	tempRowVector.add(super.ROW_SAVED);
	tempRowVector.add("keep copy of inbound infected FTP files");
	tempRowVector.add( virusSettings.getFtpInbound().getCopyOnBlock() );
	tempRowVector.add( virusSettings.getFtpInbound().getCopyOnBlockNotes() );
	allRows.add( tempRowVector );

	// keep copy of outbound infected ftp files
        tempRowVector = new Vector(4);
	tempRowVector.add(new Integer(5));
	tempRowVector.add(super.ROW_SAVED);
	tempRowVector.add("keep copy of outbound infected FTP files");
	tempRowVector.add( virusSettings.getFtpOutbound().getCopyOnBlock() );
	tempRowVector.add( virusSettings.getFtpOutbound().getCopyOnBlockNotes() );
	allRows.add( tempRowVector );

	// keep copy of inbound infected http files
	tempRowVector = new Vector(4);
	tempRowVector.add(new Integer(6));
	tempRowVector.add(super.ROW_SAVED);
	tempRowVector.add("keep copy of inbound infected http files");
	tempRowVector.add( virusSettings.getHttpInbound().getCopyOnBlock() );
	tempRowVector.add( virusSettings.getHttpInbound().getCopyOnBlockNotes() );
	allRows.add( tempRowVector );

	// keep copy of outbound infected http files
	tempRowVector = new Vector(4);
	tempRowVector.add(new Integer(7));
	tempRowVector.add(super.ROW_SAVED);
	tempRowVector.add("keep copy of outbound infected http files");
	tempRowVector.add( virusSettings.getHttpOutbound().getCopyOnBlock() );
	tempRowVector.add( virusSettings.getHttpOutbound().getCopyOnBlockNotes() );
	allRows.add( tempRowVector );
	*/

        return allRows;
    }
}
