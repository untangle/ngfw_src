/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.widgets.coloredTable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.widgets.editTable.*;


public class MColoredJTable extends JTable {

    private MColoredTableCellEditor tableCellEditor;
    private MColoredTableCellRenderer tableCellRenderer;

    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);

    public MColoredJTable() {
        super();
        setRowHeight(25);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(1,1));
        setOpaque(true);
        setDoubleBuffered(true);
        setBackground(TABLE_BACKGROUND_COLOR);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        ((JLabel)getTableHeader().getDefaultRenderer()).setPreferredSize(new Dimension(0,34));

        JLabel headerRendererJLabel = (JLabel) this.getTableHeader().getDefaultRenderer();
        headerRendererJLabel.setHorizontalAlignment( SwingConstants.CENTER );

        tableCellEditor = new MColoredTableCellEditor(this);
        tableCellRenderer = new MColoredTableCellRenderer();
    }



    public TableCellRenderer getCellRenderer(int row, int col){
        return tableCellRenderer;
    }

    public TableCellEditor getCellEditor(){
        return tableCellEditor;
    }

    public TableCellEditor getCellEditor(int row, int col){
        return tableCellEditor;
    }

    public void doGreedyColumn(int scrollPanelWidth){
        int greedyCol = ((MSortedTableModel) getModel()).getGreedyColumnViewIndex();
        if( greedyCol < 0 )
            return;
        int currentTableWidth = getWidth();
        TableColumn greedyColumn = getColumnModel().getColumn(greedyCol);
        int greedyColumnWidth = greedyColumn.getPreferredWidth();
        greedyColumn.setPreferredWidth( greedyColumnWidth + (scrollPanelWidth-currentTableWidth) );
    }

}
