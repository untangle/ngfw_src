/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.editTable;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.lang.reflect.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

/**
 * TableSorter is a decorator for TableModels; adding sorting
 * functionality to a supplied TableModel. TableSorter does
 * not store or copy the data in its TableModel; instead it maintains
 * a map from the row indexes of the view to the row indexes of the
 * model. As requests are made of the sorter (like getValueAt(row, col))
 * they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way,
 * the TableSorter appears to hold another copy of the table
 * with the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model,
 * just as the JTable itself would. Events recieved from the model
 * are examined, sometimes manipulated (typically widened), and then
 * passed on to the TableSorter's listeners (typically the JTable).
 * If a change to the model has invalidated the order of TableSorter's
 * rows, a note of this is made and the sorter will resort the
 * rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the
 * table header may be used as a complete UI for TableSorter.
 * The default renderer of the tableHeader is decorated with a renderer
 * that indicates the sorting status of each column. In addition,
 * a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns
 * and advances the sorting status of that column through three
 * values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to
 * NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns
 * and cycles the sorting status of the column through the same
 * three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except
 * that the changes to the column do not cancel the statuses of columns
 * that are already sorting - giving a way to initiate a compound
 * sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that
 * first appeared in the swing table demos in 1997.
 * 
 * @author Philip Milne
 * @author Brendon McLean 
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */

public abstract class MSortedTableModel extends DefaultTableModel implements Refreshable, Savable {
    
    // M TABLE MODEL ///////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // STRING CONSTANTS /////////////
    public static final String ROW_SAVED   = "MV_saved_";
    public static final String ROW_ADD     = "MV_add_";
    public static final String ROW_CHANGED = "MV_changed_";
    public static final String ROW_REMOVE  = "MV_remove_";
    public static final StringConstants sc = new StringConstants();
    ////////////////////////////////

    
    private int descriptionIndex = -1;
    private Vector editableVector = new Vector();
    private Vector classTypeVector = new Vector();
    private Vector defaultValueVector = new Vector();
    private Vector removableVector = new Vector();

    public abstract TableColumnModel getTableColumnModel();
    public abstract Vector generateRows(Object settings);
    public abstract void generateSettings(Object settings, boolean validateOnly) throws Exception;
    private boolean dataChanged = false;
    protected boolean getSortable(){ return true; }
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    public static final int DESCENDING = -1;
    public static final int NOT_SORTED = 0;
    public static final int ASCENDING = 1;

    private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

    public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((Comparable) o1).compareTo((Comparable)o2);
        }
    };
    public static final Comparator LEXICAL_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };
    public static final Comparator COMBOBOXMODEL_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((String)((ComboBoxModel) o1).getSelectedItem()).compareTo( (String) ((ComboBoxModel)o2).getSelectedItem()  );
        }
    };

    // MAPPINGS //////////////////////////
    private int[] colMapping = new int[24];
    private int colMappingIndex = 0;
    private Row[] viewToModel;
    private int[] modelToView;

    public int getColumnViewToModelIndex(int col){
	return colMapping[col];
    }

    private Row[] getViewToModel() {
        if (viewToModel == null) {
            int tableModelRowCount = this.getRowCount();
            viewToModel = new Row[tableModelRowCount];
            for (int row = 0; row < tableModelRowCount; row++) {
                viewToModel[row] = new Row(row);
            }

            if (isSorting()) {
                Arrays.sort(viewToModel);
            }
        }
        return viewToModel;
    }

    public int modelIndex(int viewIndex) {
        return getViewToModel()[viewIndex].modelIndex;
    }

    public int viewIndex(int modelIndex){
	return getModelToView()[modelIndex];
    }

    private int[] getModelToView() {
        if (modelToView == null) {
            int n = getViewToModel().length;
            modelToView = new int[n];
            for (int i = 0; i < n; i++) {
                modelToView[modelIndex(i)] = i;
            }
        }
        return modelToView;
    }

    private int stateIndex = -1;
    private int getStateIndex(){
	if(stateIndex == -1){
	    if( getColumnClass(0) == String.class )
		stateIndex = 0;
	    else
		stateIndex = 1;
	}
	return stateIndex;
    }

    private int indexIndex = -1;
    public int getIndexIndex(){
	if(indexIndex == -1){
	    if( getColumnClass(0) == Integer.class )
		indexIndex = 0;
	    else
		indexIndex = 1;
	}
	
	return indexIndex;
    }

    //////////////////////////////////////

    private JTableHeader tableHeader;
    private MouseListener mouseListener;
    private TableModelListener tableModelListener;
    private Map columnComparators = new HashMap();
    private List sortingColumns = new ArrayList();

    
    public MSortedTableModel() {
        this.mouseListener = new MouseHandler();
        this.tableModelListener = new TableModelHandler();
        addTableModelListener(tableModelListener);
        //addTableModelListener(this);
    }

    //    public boolean dataChanged(){ return dataChanged; }
    
    private void clearSortingState() {
        viewToModel = null;
        modelToView = null;
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != null) {
            this.tableHeader.removeMouseListener(mouseListener);
            TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
            if (defaultRenderer instanceof SortableHeaderRenderer) {
                this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer) defaultRenderer).tableCellRenderer);
            }
        }
        this.tableHeader = tableHeader;
        if (this.tableHeader != null) {
            this.tableHeader.addMouseListener(mouseListener);
            this.tableHeader.setDefaultRenderer(
                    new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
        }
    }

    public boolean isSorting() {
        return sortingColumns.size() != 0;
    }

    private Directive getDirective(int column) {
        for (int i = 0; i < sortingColumns.size(); i++) {
            Directive directive = (Directive)sortingColumns.get(i);
            if (directive.column == column) {
                return directive;
            }
        }
        return EMPTY_DIRECTIVE;
    }

    public boolean isSorting(int column){
	return getSortingStatus(column) != this.NOT_SORTED ? true : false;
    }

    public int getSortingStatus(int column) {
        return getDirective(column).direction;
    }

    private void sortingStatusChanged() {
        clearSortingState();
        fireTableDataChanged();
        if (tableHeader != null) {
            tableHeader.repaint();
        }
    }

    public void setSortingStatus(int column, int status) {
        Directive directive = getDirective(column);
        if (directive != EMPTY_DIRECTIVE) {
            sortingColumns.remove(directive);
        }
        if (status != NOT_SORTED) {
            sortingColumns.add(new Directive(column, status));
        }
        sortingStatusChanged();
    }

    protected Icon getHeaderRendererIcon(int column, int size) {
        Directive directive = getDirective(column);
        if (directive == EMPTY_DIRECTIVE) {
            return null;
        }
        return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
    }

    private void cancelSorting() {
        sortingColumns.clear();
        sortingStatusChanged();
    }

    public void setColumnComparator(Class type, Comparator comparator) {
        if (comparator == null) {
            columnComparators.remove(type);
        } else {
            columnComparators.put(type, comparator);
        }
    }

    protected Comparator getComparator(int column) {
        Class columnType = this.getColumnClass(column);
        Comparator comparator = (Comparator) columnComparators.get(columnType);
        if (comparator != null) {
            return comparator;
        }
        else if (Comparable.class.isAssignableFrom(columnType)) {
            return COMPARABLE_COMPARATOR;
        }
        else if(columnType.equals(ComboBoxModel.class)){
            return COMBOBOXMODEL_COMPARATOR;
        }
        else{
            return LEXICAL_COMPARATOR;
        }
    }

    public void addTableColumn(DefaultTableColumnModel defaultTableColumnModel,
                               int index, int minWidth,
                               boolean isResizable, boolean isEditable, boolean isRemovable, boolean isDescription,
                               Class classType, Object defaultValue, String headerTitle){

        TableColumn tableColumn;
        tableColumn = new TableColumn(index);
        tableColumn.setHeaderValue(headerTitle);
        tableColumn.setResizable(isResizable);
        tableColumn.setMinWidth(minWidth);
        if(isResizable){
            tableColumn.setMaxWidth(10000);
            tableColumn.setPreferredWidth(minWidth);
        }
        else{
            tableColumn.setMaxWidth(minWidth);
            tableColumn.setPreferredWidth(minWidth);
        }
        defaultTableColumnModel.addColumn(tableColumn);
        
        if(isDescription)
             this.descriptionIndex = index;
        if( isRemovable )
            removableVector.add(headerTitle);
        
        
        if( editableVector.size() <= index )
            editableVector.setSize(index+1);
        if(!isRemovable){
	    editableVector.set(index, new Boolean(isEditable));
        }
        else{
	    editableVector.set(index, new Boolean(false));
        }
        
        if( classTypeVector.size() <= index )
            classTypeVector.setSize(index+1);
        classTypeVector.set(index, classType);
        if( defaultValueVector.size() <= index )
            defaultValueVector.setSize(index+1);
        defaultValueVector.set(index, defaultValue);

	if(!isRemovable){
	    colMapping[colMappingIndex] = index;
	    colMappingIndex++;
	}

    }
    

    
    public void setRowChanged(int row) {
	int stateIndex = getStateIndex();
        if( !getValueAt(row, stateIndex).equals(ROW_ADD) )
            setValueAt(ROW_CHANGED, row, stateIndex);
        dataChanged = true;
    }
    
    public String getRowState(int row){
	return (String) getValueAt(row, getStateIndex());
    }
    
    public void setRowState(String state, int row){
        setValueAt(state, row, getStateIndex());
        dataChanged = true;
    }
    
    public void fillColumn(int selectedRow, int selectedColumn){
        if( !this.getColumnClass( colMapping[selectedColumn] ).equals(Boolean.class) )
            return;
            
        boolean fillValue = ((Boolean) this.getValueAt( selectedRow, colMapping[selectedColumn] )).booleanValue();
        //System.err.println("SELECTING: row " + selectedRow + " col " + selectedColumn);
        int rowCount = this.getRowCount();
        for(int i=0; i<rowCount; i++){
            if( fillValue != ((Boolean) this.getValueAt( i, colMapping[selectedColumn] )).booleanValue() ){
                this.setValueAt(new Boolean(fillValue), i, colMapping[selectedColumn]);
                this.setRowChanged(i);
            }
        }
        //this.fireTableDataChanged();
        
    }
    
    public Vector generateNewRow(int rowIndex) {
        Class newClass;
        Object defaultValue;
        Vector newRow = new Vector();
        Iterator defaultValueIterator = defaultValueVector.iterator();
        Iterator classTypeIterator = classTypeVector.iterator();

        // deal with first column
        defaultValue = defaultValueIterator.next();
        newClass = (Class) classTypeIterator.next();
        if(newClass == Integer.class){
            newRow.add(new Integer(rowIndex));
        }
        else{
            newRow.add(this.ROW_ADD);
        }
        
        // deal with second column
        defaultValue = defaultValueIterator.next();
        newClass = (Class) classTypeIterator.next();
        if(newClass == Integer.class){
            newRow.add(new Integer(rowIndex));
        }
        else{
            newRow.add(this.ROW_ADD);
        }
        
        
        while(true){
            if( defaultValueIterator.hasNext() && classTypeIterator.hasNext() ){
                Object newField = null;
                defaultValue = defaultValueIterator.next();
                newClass = (Class) classTypeIterator.next();

                
                if(newClass == ComboBoxModel.class){
                    ComboBoxModel tempModel = (ComboBoxModel) defaultValue;
                    DefaultComboBoxModel newModel = new DefaultComboBoxModel();
                    for(int i=0; i<tempModel.getSize(); i++)
                        newModel.addElement( tempModel.getElementAt(i) );
                    newModel.setSelectedItem( tempModel.getSelectedItem() );
                    newField = newModel;
                }
                else{
                    try{
			if(defaultValue != null){
			    Constructor newClassConstructor = newClass.getConstructor( new Class[] {defaultValue.getClass()} );
			    newField = newClassConstructor.newInstance( new Object[] {defaultValue} );
			}
			else{
			    Constructor newClassConstructor = newClass.getConstructor( new Class[] {} );
			    newField = newClassConstructor.newInstance( new Object[] {} );
			}
                    }
                    catch(Exception e){
                        Util.handleExceptionNoRestart("error generating row", e);
                        return null;
                    }
                }

                newRow.add( newField );
            }
            else if( !defaultValueIterator.hasNext() && !classTypeIterator.hasNext() ){
                return newRow;
            }
            else if( defaultValueIterator.hasNext() && !classTypeIterator.hasNext() ){
                return null;
            }
            else if( !defaultValueIterator.hasNext() && classTypeIterator.hasNext() ){
                return null;
            }
            else{
                return null;
            }
        }

    }
    //// TableModel interface methods //////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////    
    
    public void flushLog(){
        
    }
    
    public boolean isCellEditable(int row, int col) {
        return ((Boolean)editableVector.elementAt( col )).booleanValue();
    }

    public Object getValueAt(int row, int col) {
        //System.err.println("GETting value at row: " + row + "( " + modelIndex(row) + " )" + " col: " + col);
        return super.getValueAt(modelIndex(row), col ); 
    }

    public void setValueAt(Object aValue, int row, int col) {
        // System.err.println("SETting value at row: " + row + "( " + modelIndex(row) + " )" + " col: " + col + " (" + aValue.toString() + ")");
	int changedModelRow = modelIndex(row);

        super.setValueAt(aValue, modelIndex(row), col );

	if( isSorting(col) ){
	    int newRow = viewIndex(changedModelRow);
	    tableHeader.getTable().clearSelection();
	    tableHeader.getTable().getSelectionModel().addSelectionInterval(newRow, newRow);
	    Rectangle selectionRect = tableHeader.getTable().getCellRect(newRow, 0, true);
	    tableHeader.getTable().scrollRectToVisible(selectionRect);
	}

        dataChanged = true;
    }
    
        
    public Class getColumnClass(int col){
	Class returnClass = null;
	try{
	    returnClass = (Class) classTypeVector.elementAt(col);
	}
	catch(Exception e){
	}
	return returnClass;
    }
    
    // add a row
    public void insertNewRow(int index) {
	int indexIndex = getIndexIndex();
        for(int i=index; i<this.getRowCount(); i++){
            this.setValueAt(new Integer(i+2), i, indexIndex);
	}
        super.getDataVector().insertElementAt(generateNewRow(index+1), index);
        this.fireTableDataChanged();
        dataChanged = true;
    }
        
    // remove a row
    public void removeSelectedRows(int[] indexes){
	int stateIndex = getStateIndex();
        for(int i=0; i< indexes.length; i++){
            this.setValueAt(this.ROW_REMOVE, indexes[i], stateIndex);
        }

        this.fireTableDataChanged();
        dataChanged = true;
    }
    
    public Vector<Vector> getOriginalDataVector(){
	return super.getDataVector();
    }

    public Vector<Vector> getDataVector(){
	int stateIndex = getStateIndex();
	Vector filteredData = new Vector();
	for( Vector rowVector : (Vector<Vector>) super.getDataVector() ){
	    if( !((String)rowVector.elementAt(stateIndex)).equals(ROW_REMOVE) )
		filteredData.add(rowVector);
	}
	return filteredData;
    }
    
    // tell the table to reload its data set from the server
    public void doRefresh(Object settings){

	this.getTableHeader().getTable().getCellEditor().stopCellEditing();
	this.getTableHeader().getTable().clearSelection();
	
	Vector dataVector = generateRows( settings );
	super.getDataVector().removeAllElements();
	super.getDataVector().addAll(dataVector);
	this.fireTableDataChanged();
    }
    
    // save the data from the table, and tell the table to reflect that the data has been saved
    public void doSave(Object settings, boolean validateOnly) throws Exception {

	if(Util.getIsDemo())
            return;

	this.getTableHeader().getTable().getCellEditor().stopCellEditing();
	this.getTableHeader().getTable().clearSelection();
	generateSettings(settings, validateOnly);
    }
    
    public String getDescription(int rowIndex) {
        if( (descriptionIndex < 0) || (rowIndex < 0) )
            return "[no description]";
        else{
            return (String) super.getValueAt(modelIndex(rowIndex), this.descriptionIndex);
        }
    }
    
    public void hideColumns(JTable jTable){
	int initialColCount = this.getColumnCount();
        Iterator columnIterator = removableVector.iterator();
        while(columnIterator.hasNext())
            jTable.removeColumn(jTable.getColumn( (String) columnIterator.next())  );

	int finalColCount = this.getColumnCount();
    }
        
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    
    // Helper classes
    public ComboBoxModel copyComboBoxModel(ComboBoxModel comboBoxModel){
        DefaultComboBoxModel newComboBoxModel = new DefaultComboBoxModel();
        for(int i=0; i<comboBoxModel.getSize(); i++){
            newComboBoxModel.insertElementAt(comboBoxModel.getElementAt(i), i);
        }
        newComboBoxModel.setSelectedItem(comboBoxModel.getSelectedItem());
        return newComboBoxModel;
    }
    
    public ComboBoxModel generateComboBoxModel(Object[] options, Object setting){
        DefaultComboBoxModel returnComboBoxModel = new DefaultComboBoxModel();
        // Don't need to sort any more because the enumerations should be sorted from the server.
        // Arrays.sort(options);
        for(int i=0; i < options.length; i++){
            returnComboBoxModel.insertElementAt(options[i].toString(), i);
        }
        returnComboBoxModel.setSelectedItem(setting.toString());
        
        return returnComboBoxModel;
    }
    
    private class Row implements Comparable {
        private int modelIndex;

        public Row(int index) {
            this.modelIndex = index;
        }

        public int compareTo(Object o) {
            int row1 = modelIndex;
            int row2 = ((Row) o).modelIndex;

            for (Iterator it = sortingColumns.iterator(); it.hasNext();) {
                Directive directive = (Directive) it.next();
                int column = directive.column;
                Object o1 = MSortedTableModel.super.getValueAt(row1, column);
                Object o2 = MSortedTableModel.super.getValueAt(row2, column);

                int comparison = 0;
                // Define null less than everything, except null.
                if (o1 == null && o2 == null) {
                    comparison = 0;
                } else if (o1 == null) {
                    comparison = -1;
                } else if (o2 == null) {
                    comparison = 1;
                } else {
                    comparison = getComparator(column).compare(o1, o2);
                }
                if (comparison != 0) {
                    return directive.direction == DESCENDING ? -comparison : comparison;
                }
            }
            return 0;
        }
    }

    private class TableModelHandler implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            // System.err.println("changed col: " + e.getColumn() );
            // show that the row has been changed
            if( e.getType() == TableModelEvent.UPDATE ){
		if( e.getColumn() == getStateIndex() )
		    return;
	    }
                //MSortedTableModel.this.setValueAt(ROW_CHANGED, e.getFirstRow(), 1);

             
            // If we're not sorting by anything, just pass the event along.             
            if (!isSorting()) {
                clearSortingState(); 
                //fireTableChanged(e);
                return;
            }
                
            // If the table structure has changed, cancel the sorting; the             
            // sorting columns may have been either moved or deleted from             
            // the model. 
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                cancelSorting();
                //fireTableChanged(e);
                return;
            }

            // We can map a cell event through to the view without widening             
            // when the following conditions apply: 
            // 
            // a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and, 
            // b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
            // c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and, 
            // d) a reverse lookup will not trigger a sort (modelToView != null)
            //
            // Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
            // 
            // The last check, for (modelToView != null) is to see if modelToView 
            // is already allocated. If we don't do this check; sorting can become 
            // a performance bottleneck for applications where cells  
            // change rapidly in different parts of the table. If cells 
            // change alternately in the sorting column and then outside of             
            // it this class can end up re-sorting on alternate cell updates - 
            // which can be a performance problem for large tables. The last 
            // clause avoids this problem. 
            /*
            int column = e.getColumn();
            if (e.getFirstRow() == e.getLastRow()
                    && column != TableModelEvent.ALL_COLUMNS
                    && getSortingStatus(column) == NOT_SORTED
                    && modelToView != null) {
                int viewIndex = getModelToView()[e.getFirstRow()];
                fireTableChanged(new TableModelEvent(MSortedTableModel.this, 
                                                     viewIndex, viewIndex, 
                                                     column, e.getType()));
                return;
            }
            */
            // Something has happened to the data that may have invalidated the row order. 
            clearSortingState();
            //fireTableDataChanged();
            return;
        }
    }

    private class MouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
	    if ( e.getClickCount() != 1 ){
		return;
	    }
	    else if( !MSortedTableModel.this.getSortable() )
		return;
	    

            JTableHeader h = (JTableHeader) e.getSource();
	    
	    int selectedRow = h.getTable().getSelectedRow();
	    int mappingRow = -1;
	    if( selectedRow != -1 ){
		mappingRow = MSortedTableModel.this.modelIndex(selectedRow);
		// System.err.print("--> selected visual row: " + selectedRow + " which maps to data row: " +  mappingRow );
		if( h.getTable().isEditing() )
		    ((DefaultCellEditor)h.getTable().getCellEditor()).stopCellEditing();
	    }

            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = columnModel.getColumn(viewColumn).getModelIndex();
            
            Class columnClass = MSortedTableModel.this.getColumnClass(column);
            
            if(    (column == -1)
                || (MPasswordField.class.equals(columnClass))
                || (ImageIcon.class.equals(columnClass))
                || (Object.class.equals(columnClass))   ){
                    return;
            }
            

            int status = getSortingStatus(column);
            if (!e.isControlDown()) {
                cancelSorting();
            }
            // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or 
            // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed. 
            status = status + (e.isShiftDown() ? -1 : 1);
            status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
            setSortingStatus(column, status);

	    if( selectedRow != -1){
		int newRow = MSortedTableModel.this.viewIndex(mappingRow);
		// System.err.println("New row is: " + newRow);
		h.getTable().clearSelection();
		h.getTable().getSelectionModel().addSelectionInterval(newRow, newRow);
		Rectangle selectionRect = h.getTable().getCellRect(newRow, 0, true);
		h.getTable().scrollRectToVisible(selectionRect);
	    }

        }
    }

    private static class Arrow implements Icon {
        private boolean descending;
        private int size;
        private int priority;

        public Arrow(boolean descending, int size, int priority) {
            this.descending = descending;
            this.size = size;
            this.priority = priority;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color color = c == null ? Color.GRAY : c.getBackground();             
            // In a compound sort, make each succesive triangle 20% 
            // smaller than the previous one. 
            int dx = (int)(size/2*Math.pow(0.8, priority));
            int dy = descending ? dx : -dx;
            // Align icon (roughly) with font baseline. 
            y = y + 5*size/6 + (descending ? -dy : 0);
            int shift = descending ? 1 : -1;
            g.translate(x, y);

            // Right diagonal. 
            g.setColor(color.darker());
            g.drawLine(dx / 2, dy, 0, 0);
            g.drawLine(dx / 2, dy + shift, 0, shift);
            
            // Left diagonal. 
            g.setColor(color.brighter());
            g.drawLine(dx / 2, dy, dx, 0);
            g.drawLine(dx / 2, dy + shift, dx, shift);
            
            // Horizontal line. 
            if (descending) {
                g.setColor(color.darker().darker());
            } else {
                g.setColor(color.brighter().brighter());
            }
            g.drawLine(dx, 0, 0, 0);

            g.setColor(color);
            g.translate(-x, -y);
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }

    private class SortableHeaderRenderer implements TableCellRenderer {
        private TableCellRenderer tableCellRenderer;

        public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        public Component getTableCellRendererComponent( JTable table, 
                                                       Object value,
                                                       boolean isSelected, 
                                                       boolean hasFocus,
                                                       int row, 
                                                       int column) {
            Component c = tableCellRenderer.getTableCellRendererComponent(table, 
                    value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalTextPosition(JLabel.LEFT);
                int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
            }
            return c;
        }
    }

    private static class Directive {
        private int column;
        private int direction;

        public Directive(int column, int direction) {
            this.column = column;
            this.direction = direction;
        }
    }
}
