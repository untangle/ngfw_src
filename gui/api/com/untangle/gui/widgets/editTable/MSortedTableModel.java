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

package com.untangle.gui.widgets.editTable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.uvm.node.*;
import org.apache.log4j.Logger;

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

public abstract class MSortedTableModel<T> extends DefaultTableModel
    implements Refreshable<T>, Savable<T> {

    private final Logger logger = Logger.getLogger(getClass());

    public MSortedTableModel() {
        this.mouseListener = new MouseHandler();
        addTableModelListener(new TableModelHandler());
    }

    // STRING CONSTANTS /////////////
    public static final String ROW_SAVED   = "MV_saved_";
    public static final String ROW_ADD     = "MV_add_";
    public static final String ROW_CHANGED = "MV_changed_";
    public static final String ROW_REMOVE  = "MV_remove_";
    public static final StringConstants sc = new StringConstants();
    ////////////////////////////////

    // MODES ///////////////////////
    private boolean doInstantRemove = false;
    private boolean alwaysSelectable = false;
    private boolean alwaysEditable = true;
    ////////////////////////////////

    // COLUMN SETUP ////////////////
    private Vector editableVector = new Vector();
    private Vector classTypeVector = new Vector();
    private Vector defaultValueVector = new Vector();
    private Vector removableVector = new Vector();
    ////////////////////////////////

    // ABSTRACT METHODS /////////////
    public abstract TableColumnModel getTableColumnModel();
    public abstract Vector<Vector> generateRows(T compoundSettings);
    public abstract void generateSettings(T compoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception;
    /////////////////////////////////

    // COMPARATORS //////////////////////
    private Map columnComparators = new HashMap();
    public static final int DESCENDING = -1;
    public static final int NOT_SORTED = 0;
    public static final int ASCENDING = 1;

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }

    // PROTECTEDS ///////////////
    protected void wireUpNewRow(Vector rowVector) {}
    protected boolean getSortable(){ return true; }
    public void handleDependencies(int modelCol, int modelRow){ fireTableRowsUpdated(modelRow, modelRow); }

    private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

    public static final Comparator COMPARABLE_COMPARATOR = new Comparator() {
            public int compare(Object o1, Object o2) {
                if( o1 instanceof String && o2 instanceof String )
                    return ((String)o1).compareToIgnoreCase((String)o2);
                else
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
                Object selectedObject1 = ((ComboBoxModel)o1).getSelectedItem();
                Object selectedObject2 = ((ComboBoxModel)o2).getSelectedItem();
                if( (selectedObject1 instanceof Comparable) && (selectedObject2 instanceof Comparable) )
                    return ((Comparable)selectedObject1).compareTo(selectedObject2);
                else
                    return selectedObject1.toString().compareTo(selectedObject2.toString());
            }
        };
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
    //////////////////////////////////////

    // MODEL-VIEW MAPPINGS ///////////////
    private Vector<Integer> colViewToModelMapping = new Vector();
    private Vector<Integer> colModelToViewMapping = new Vector();
    private Row<?>[] rowViewToModelMapping;
    private int[] rowModelToViewMapping;

    public int getColViewToModelIndex(int viewIndex){
        return colViewToModelMapping.elementAt(viewIndex);
    }
    public int getColModelToViewIndex(int modelIndex){
        return colModelToViewMapping.elementAt(modelIndex);
    }
    public int getRowViewToModelIndex(int viewIndex) {
        if (rowViewToModelMapping == null) {
            int tableModelRowCount = this.getRowCount();
            rowViewToModelMapping = new Row[tableModelRowCount];
            for (int row = 0; row < tableModelRowCount; row++) {
                rowViewToModelMapping[row] = new Row(row);
            }
            if (isSorting()) {
                Arrays.sort(rowViewToModelMapping);
            }
        }
        return rowViewToModelMapping[viewIndex].modelIndex;
    }
    public int getRowModelToViewIndex(int modelIndex) {
        if (rowModelToViewMapping == null) {
            int tableModelRowCount = this.getRowCount();
            rowModelToViewMapping = new int[tableModelRowCount];
            for (int row = 0; row < tableModelRowCount; row++) {
                rowModelToViewMapping[getRowViewToModelIndex(row)] = row;
            }
        }
        return rowModelToViewMapping[modelIndex];
    }
    ///////////////////////////////////////

    // INDEXES /////////////////////////////
    private int stateModelIndex = 0;  // usually the first col
    private int descriptionModelIndex = -1;  // defaults to no description
    private int orderModelIndex = 1;  // defaults to no order shown
    private int greedyColumnModelIndex = -1;

    public int getStateModelIndex(){ return stateModelIndex; }
    public int getDescriptionModelIndex(){ return descriptionModelIndex; }
    public int getOrderModelIndex(){ return orderModelIndex; }
    public int getOrderViewIndex(){ return getColModelToViewIndex(orderModelIndex); }
    public void setOrderModelIndex(int index){ orderModelIndex = index; };
    public int getGreedyColumnViewIndex(){
        if( greedyColumnModelIndex == -1 )
            return -1;
        else
            return getColModelToViewIndex(greedyColumnModelIndex);
    }
    public String getDescription(int row) {
        if( (descriptionModelIndex < 0) || (row < 0) )
            return "[no description]";
        else if( getDataVector().size() <= 0 ){
            return "[no description]";
        }
        else{
            return (String) super.getValueAt(row, descriptionModelIndex);
        }
    }
    //////////////////////////////////////

    // HEADER //////////////////////////
    private JTableHeader tableHeader;
    private MouseListener mouseListener;

    public void hideColumns(JTable jTable){
        int initialColCount = this.getColumnCount();
        Iterator columnIterator = removableVector.iterator();
        while(columnIterator.hasNext())
            jTable.removeColumn(jTable.getColumn( (String) columnIterator.next())  );

        int finalColCount = this.getColumnCount();
    }
    ///////////////////////////////////

    // SORTING //////////////////////////
    private List sortingColumns = new ArrayList();

    public JTableHeader getTableHeader() {
        return tableHeader;
    }
    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != null) {
            this.tableHeader.removeMouseListener(mouseListener);
            TableCellRenderer defaultRenderer = this.tableHeader.getDefaultRenderer();
            if (defaultRenderer.getClass().getName().endsWith("SortableHeaderRenderer")) {
                this.tableHeader.setDefaultRenderer(((SortableHeaderRenderer) defaultRenderer).tableCellRenderer);
            }
        }
        this.tableHeader = tableHeader;
        if (this.tableHeader != null) {
            this.tableHeader.addMouseListener(mouseListener);
            this.tableHeader.setDefaultRenderer(new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
        }
    }
    private void clearSortingState() {
        rowViewToModelMapping = null;
        rowModelToViewMapping = null;
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
    private void cancelSorting() {
        sortingColumns.clear();
        sortingStatusChanged();
    }
    ////////////////////////////////

    // COLUMN/ROW CREATION //////////
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

        if(isDescription){
            this.descriptionModelIndex = index;
            if( !isRemovable )
                this.greedyColumnModelIndex = index;
        }
        if( isRemovable )
            removableVector.add(headerTitle);


        if( editableVector.size() <= index )
            editableVector.setSize(index+1);
        if(!isRemovable){
            editableVector.set(index, isEditable);
        }
        else{
            editableVector.set(index, false);
        }

        if( classTypeVector.size() <= index )
            classTypeVector.setSize(index+1);
        classTypeVector.set(index, classType);
        if( defaultValueVector.size() <= index )
            defaultValueVector.setSize(index+1);
        defaultValueVector.set(index, defaultValue);

        if(isRemovable){
            colModelToViewMapping.add(-1);
        }
        else{
            colViewToModelMapping.add(index);
            colModelToViewMapping.add(colViewToModelMapping.size()-1);
        }

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
            newRow.add(rowIndex);
        }
        else{
            newRow.add(this.ROW_ADD);
        }

        // deal with second column
        defaultValue = defaultValueIterator.next();
        newClass = (Class) classTypeIterator.next();
        if(newClass == Integer.class){
            newRow.add(rowIndex);
        }
        else{
            newRow.add(this.ROW_ADD);
        }

        while(true){
            if( defaultValueIterator.hasNext() && classTypeIterator.hasNext() ){
                Object newField = null;
                defaultValue = defaultValueIterator.next();
                newClass = (Class) classTypeIterator.next();

                if( newClass == ComboBoxModel.class ){
                    newField = copyComboBoxModel( (ComboBoxModel) defaultValue );
                }
                else if( newClass == Date.class ){
                    newField = new Date(0l);  // rendered as a blank
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
                break;
            }
            else if( defaultValueIterator.hasNext() && !classTypeIterator.hasNext() ){
                break;
            }
            else if( !defaultValueIterator.hasNext() && classTypeIterator.hasNext() ){
                break;
            }
        }
        // WIRE DAT SHISE UP
        wireUpNewRow(newRow);
        return newRow;
    }
    ////////////////////////////////

    // ROW OPERATIONS //////////////
    public void setInstantRemove(boolean enabled){
        doInstantRemove = enabled;
    }
    public void setAlwaysSelectable(boolean enabled){
        alwaysSelectable = enabled;
    }
    public boolean getAlwaysSelectable(){
        return alwaysSelectable;
    }
    public void setAlwaysEditable(boolean enabled){
        alwaysEditable = enabled;
    }
    public boolean getAlwaysEditable(){
        return alwaysEditable;
    }
    public void moveRow(final int fromModelRow, final int toModelRow){
        if( fromModelRow != toModelRow ){
            // CREATE THE NEW INDEXES
            int orderModelIndex = getOrderModelIndex();
            int newIndex;
            if( fromModelRow < toModelRow){
                for(int i=fromModelRow+1; i<=toModelRow; i++)
                    changeRow(i, orderModelIndex, i);
            }
            else{ // fromModelRow > toModelRow
                for(int i=toModelRow; i<fromModelRow; i++){
                    changeRow(i, orderModelIndex, i+2);
                }
            }
            // UPDATE THE MODEL
            Vector<Vector> dataVector = getDataVector();
            Vector movedRow = dataVector.remove(fromModelRow);
            dataVector.add(toModelRow, movedRow);
            fireTableDataChanged();
        }
        // HIGHLIGHT NEW ROW
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            int newViewRow = getRowModelToViewIndex(toModelRow);
            JTable table = getTableHeader().getTable();
            table.clearSelection();
            table.getSelectionModel().addSelectionInterval(newViewRow, newViewRow);
            // SCROLL TO ROW
            Rectangle rect = table.getCellRect(newViewRow, 0, true);
            table.scrollRectToVisible(rect);
        }});
    }
    public void fillColumn(int modelRow, int modelCol){
        if( !getColumnClass( modelCol ).equals(Boolean.class) )
            return;
        boolean fillValue = (Boolean) super.getValueAt( modelRow, modelCol );
        int rowCount = getRowCount();
        for(int i=0; i<rowCount; i++){
            if( fillValue !=  super.getValueAt( i, modelCol ) ){
                changeRow(i, modelCol, fillValue);
            }
        }
        fireTableDataChanged();
    }
    private void changeRow(int modelRow, int modelCol, Object value) {
        Vector<Vector> dataVector = getDataVector();
        Vector changedRow = dataVector.elementAt(modelRow);
        String state = (String) changedRow.elementAt(stateModelIndex);
        if( !ROW_ADD.equals(state) )
            changedRow.setElementAt(ROW_CHANGED, stateModelIndex);
        changedRow.setElementAt(value, modelCol);
    }
    public void insertNewRow(int modelRow) {
        int rowCount = getRowCount();
        Vector movedRow;
        Vector<Vector> dataVector = getDataVector();
        for(int i=modelRow; i<rowCount; i++){
            movedRow = dataVector.elementAt(i);
            movedRow.setElementAt(i+2, orderModelIndex);
        }
        dataVector.insertElementAt(generateNewRow(modelRow+1), modelRow);
        fireTableRowsInserted(modelRow, modelRow);
        if( settingsChangedListener != null )
            settingsChangedListener.settingsChanged(this);
    }
    public void insertNewRow(int modelRow, Vector newRow){
        int rowCount = getRowCount();
        Vector movedRow;
        Vector<Vector> dataVector = getDataVector();
        for(int i=modelRow; i<rowCount; i++){
            movedRow = dataVector.elementAt(i);
            movedRow.setElementAt(i+2, orderModelIndex);
        }
        dataVector.insertElementAt(newRow, modelRow);
        fireTableRowsInserted(modelRow, modelRow);
        if( settingsChangedListener != null )
            settingsChangedListener.settingsChanged(this);
    }


    public void clearAllRows(){
        this.getTableHeader().getTable().getCellEditor().stopCellEditing();
        this.getTableHeader().getTable().clearSelection();
        Vector<Vector> dataVector = getDataVector();
        dataVector.removeAllElements();
        fireTableDataChanged();
    }
    public void removeSelectedRows(int[] modelRows){
        Vector<Vector> dataVector = getDataVector();
        Vector removedRow;
        Vector reindexedRow;
        if( doInstantRemove ){
            Vector modelEntries[] = new Vector[modelRows.length];
            // XXX not fast, but works for now
            for(int i=0; i<modelRows.length; i++){
                modelEntries[i] = dataVector.get(modelRows[i]);
            }
            for(int i=0; i<modelEntries.length; i++){
                dataVector.remove(modelEntries[i]);
            }
            for(int i=0; i<dataVector.size(); i++){
                reindexedRow = dataVector.elementAt(i);
                reindexedRow.setElementAt(i+1, orderModelIndex);
            }
            fireTableDataChanged();
        }
        else{
            for(int i=0; i< modelRows.length; i++){
                removedRow = dataVector.elementAt(modelRows[i]);
                removedRow.setElementAt(ROW_REMOVE, stateModelIndex);
            }
            fireTableRowsUpdated(modelRows[0], modelRows[modelRows.length-1]);
        }
        if( settingsChangedListener != null )
            settingsChangedListener.settingsChanged(this);
    }
    public String getRowState(int modelRow){
        //System.out.println("getting row state: (model row)  " + modelRow + " index: " + stateModelIndex);
        return (String) super.getValueAt(modelRow, stateModelIndex);
        //Vector<Vector> dataVector = getDataVector();
        //return (String) dataVector.elementAt(modelRow).elementAt(stateModelIndex);
    }
    public void setRowState(String state, int modelRow){
        Vector<Vector> dataVector = getDataVector();
        Vector row = dataVector.elementAt(modelRow);
        row.setElementAt(state, stateModelIndex);
        fireTableRowsUpdated(modelRow, modelRow);
    }
    public void setRowChanged(int modelRow, boolean isFinalChange){
        Vector<Vector> dataVector = getDataVector();
        Vector changedRow = dataVector.elementAt(modelRow);
        String state = (String) changedRow.elementAt(stateModelIndex);
        if( !ROW_ADD.equals(state) ){
            changedRow.setElementAt(ROW_CHANGED, stateModelIndex);
            fireTableRowsUpdated(modelRow, modelRow);
        }
        if( isFinalChange && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }
    ///////////////////////////////////



    //// TableModel interface override methods //////////////////////////////////////////
    // setValueAt(...) and getValueAt(...) deal in view space because that is how the JTable
    // sees the world, and reads and writed to the model... This I cannot change.
    // getColumnClass(...) and isCellEditable(...) are my bitches though, and they shall be
    // in model space.
    public void setValueAt(Object value, int viewRow, int modelCol){
        if( getAlwaysSelectable() )
            return;
        super.setValueAt(value, getRowViewToModelIndex(viewRow), modelCol);
        if( (getOrderModelIndex() != -1) && ( modelCol == getOrderModelIndex()) ){
            // deal with row order changing
            int modelRow = getRowViewToModelIndex(viewRow);
            moveRow(modelRow, ((Integer)value)-1);
        }
        if( getSortingStatus(modelCol) != NOT_SORTED ){
            fireTableDataChanged();  // because otherwise the table will only update the view of the changed column
        }
    }
    public Object getValueAt(int viewRow, int viewCol) {
        try {
            return super.getValueAt( getRowViewToModelIndex(viewRow), viewCol );
        } catch (ArrayIndexOutOfBoundsException exn) {
            logger.warn("bad column in: " + getClass()
                        + " row: " + viewRow + " col: " + viewCol
                        + " data: " + dataVector);
            throw exn;
        }
    }
    public Class getColumnClass(int modelCol){
        return (Class) classTypeVector.elementAt(modelCol);
    }
    public boolean isCellEditable(int modelRow, int modelCol) {
        if( alwaysSelectable ){
            Class targetClass = (Class) classTypeVector.elementAt(modelCol);
            if( targetClass == Integer.class )
                return false;
            else if( targetClass == Long.class )
                return false;
            else if( targetClass == Float.class )
                return false;
            else if( targetClass == Double.class )
                return false;
            else
                return true;
        }
        else
            return ((Boolean)editableVector.elementAt(modelCol)).booleanValue();
    }
    ///////////////////////////////////////////////////

    // SAVABLE / REFRESHABLE ///////////////
    public void doRefresh(T compoundSettings){
        this.getTableHeader().getTable().getCellEditor().stopCellEditing();
        this.getTableHeader().getTable().clearSelection();
        Vector<Vector> tableVector = generateRows( compoundSettings );
        getDataVector().removeAllElements();
        getDataVector().addAll(tableVector);
        fireTableDataChanged();
    }
    public void doSave(T compoundSettings, boolean validateOnly) throws Exception {
        if(Util.getIsDemo())
            return;
        this.getTableHeader().getTable().getCellEditor().stopCellEditing();
        this.getTableHeader().getTable().clearSelection();
        prevalidate(compoundSettings, getDataVector());
        generateSettings(compoundSettings, getFilteredDataVector(), validateOnly);
    }
    public void prevalidate(T compoundSettings, Vector<Vector> tableVector) throws Exception {
        // default implementation meant to do nothing
    }
    ///////////////////////////

    // HELPERS //////////////////
    public Vector<Vector> getFilteredDataVector(){
        Vector<Vector> dataVector = getDataVector();
        Vector<Vector> filteredData = new Vector();
        String state;
        for( Vector rowVector : dataVector ){
            state = (String) rowVector.elementAt(stateModelIndex);
            if( !ROW_REMOVE.equals(state) )
                filteredData.add(rowVector);
        }
        return filteredData;
    }
    public ComboBoxModel copyComboBoxModel(ComboBoxModel comboBoxModel){
        UtComboBoxModel newComboBoxModel = new UtComboBoxModel();
        int size = comboBoxModel.getSize();
        for(int i=0; i<size; i++){
            newComboBoxModel.insertElementAt(comboBoxModel.getElementAt(i), i);
        }
        newComboBoxModel.setSelectedItem(comboBoxModel.getSelectedItem());
        return newComboBoxModel;
    }
    public UtComboBoxModel generateComboBoxModel(List optionList, Object setting){
        return generateComboBoxModel(optionList.toArray(), setting);
    }
    public UtComboBoxModel generateComboBoxModel(Object[] options, Object setting){
        UtComboBoxModel returnComboBoxModel = new UtComboBoxModel();
        for( Object option : options ){
            returnComboBoxModel.addElement(option);
        }
        returnComboBoxModel.setSelectedItem(setting);
        return returnComboBoxModel;
    }
    protected Icon getHeaderRendererIcon(int column, int size) {
        Directive directive = getDirective(column);
        if (directive == EMPTY_DIRECTIVE) {
            return null;
        }
        return new Arrow(directive.direction == DESCENDING, size, sortingColumns.indexOf(directive));
    }
    ///////////////////////////


    // PRIVATE CLASSES ///////////////
    private class Row<T> implements Comparable {
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

            // show that the row has been changed
            if( e.getType() == TableModelEvent.UPDATE ){
                if( e.getColumn() == stateModelIndex ){
                    return;
                }
            }

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
            //fireTableChanged(e);
            return;
        }
    }

    private class MouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {

            // prevent multi-clicks, and sorting where sorting is not allowed
            if ( e.getClickCount() != 1 ){
                return;
            }
            else if( !MSortedTableModel.this.getSortable() ){
                return;
            }

            // get the currently selected viewRow, and translate to a model row
            JTableHeader tableHeader = (JTableHeader) e.getSource();
            int viewRow = tableHeader.getTable().getSelectedRow();
            int modelRow = -1;
            if( viewRow != -1 ){
                modelRow = MSortedTableModel.this.getRowViewToModelIndex(viewRow);
                if( tableHeader.getTable().isEditing() )
                    ((DefaultCellEditor)tableHeader.getTable().getCellEditor()).stopCellEditing();
            }

            // dont allow sorting of passwords, images, or generic objects
            TableColumnModel columnModel = tableHeader.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            if( viewColumn == -1 )
                return;
            int modelColumn = columnModel.getColumn(viewColumn).getModelIndex();
            Class columnClass = MSortedTableModel.this.getColumnClass(modelColumn);
            if(    (modelColumn == -1)
                   || (MPasswordField.class.equals(columnClass))
                   || (ImageIcon.class.equals(columnClass))
                   || (Object.class.equals(columnClass))   ){
                return;
            }

            // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
            // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
            int status = getSortingStatus(modelColumn);
            if (!e.isControlDown()) {
                cancelSorting();
            }
            status = status + (e.isShiftDown() ? -1 : 1);
            status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
            setSortingStatus(modelColumn, status);

            /*
              if( viewRow != -1 ){
              int newRow = MSortedTableModel.this.getRowViewToModelIndex(modelRow);
              tableHeader.getTable().clearSelection();
              tableHeader.getTable().getSelectionModel().addSelectionInterval(newRow, newRow);
              Rectangle selectionRect = tableHeader.getTable().getCellRect(newRow, 0, true);
              tableHeader.getTable().scrollRectToVisible(selectionRect);
              }
            */

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
    /////////////////////////////////////
}
