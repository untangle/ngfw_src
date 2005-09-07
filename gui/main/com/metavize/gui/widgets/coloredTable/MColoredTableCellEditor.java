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

package com.metavize.gui.widgets.coloredTable;

import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;


public class MColoredTableCellEditor extends DefaultCellEditor implements KeyListener, ActionListener, ChangeListener, CaretListener {
        
    private MColoredJTable mColoredJTable;
            
    private int selectedModelRow, selectedModelCol;
    private Object selectedValue, newValue;
    private Object returnValue;
    private String selectedState;
    
    private static final Font font;
    private static final MLineBorder mLineBorder;
    private static final Color focusBackgroundColor, focusBorderColor, caretColor, highlightColor, focusGrey;
    private final JCheckBox jCheckBox;
    private final JComboBox jComboBox;
    private final JTextField jTextField;
    private final JSlider jSlider;
    private final JSpinner jSpinner;
    private final MPasswordField mPasswordField;
    
    private JComponent editedComponent;
    
    private MSortedTableModel mSortedTableModel;
    
    static{
        font = new java.awt.Font("Dialog", 0, 12);
        focusBackgroundColor = new Color(150, 150, 236);
        focusBorderColor = new Color(64, 64, 236);
        focusGrey = new Color(230, 230, 230);
        mLineBorder = new MLineBorder(focusBorderColor, 2);
        caretColor = Color.BLACK;
        highlightColor = new Color(.4f, .4f, 1f);
    }
    
    public MColoredTableCellEditor(MColoredJTable mColoredJTable){
	super(new JCheckBox());
	this.mColoredJTable = mColoredJTable;
	//this.setClickCountToStart(1);
	
	
	jCheckBox = new JCheckBox();
	jCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
	jCheckBox.setOpaque(true);
	jCheckBox.setBorderPainted(true);
	jCheckBox.setBorder(mLineBorder);
	jCheckBox.setBackground(focusBackgroundColor);
	jCheckBox.setFocusable(false);
	jCheckBox.addActionListener(this);
	
	jComboBox = new JComboBox();
	jComboBox.setFont(font);
	jComboBox.setOpaque(true);
	jComboBox.setBorder(mLineBorder);
	jComboBox.setFocusable(false);
	jComboBox.addActionListener(this);
	
	jTextField = new JTextField();
	jTextField.setEditable(true);
	jTextField.setFont(font);
	jTextField.setHorizontalAlignment(JTextField.LEFT);
	jTextField.setOpaque(true);
	jTextField.setBorder(mLineBorder);
	jTextField.setBackground(focusGrey);
	jTextField.setSelectionColor(highlightColor);
	jTextField.addCaretListener(this);
	
	mPasswordField = new MPasswordField();
	mPasswordField.setEditable(true);
	mPasswordField.setFont(font);
	mPasswordField.setHorizontalAlignment(JTextField.LEFT);
	mPasswordField.setOpaque(true);
	mPasswordField.setBorder(mLineBorder);
	mPasswordField.setBackground(focusGrey); //jTextField.set
	mPasswordField.setSelectionColor(highlightColor);
	mPasswordField.addActionListener(this);
	mPasswordField.addCaretListener(this);
	
	jSlider = new JSlider();
	jSlider.setOpaque(false);
	jSlider.setFocusable(false);
	jSlider.setMajorTickSpacing(25);
	jSlider.setMinorTickSpacing(10);
	jSlider.setPaintLabels(true);
	jSlider.setFont(new java.awt.Font("Default", 0, 10));
	jSlider.setBorder(mLineBorder);
	jSlider.setBackground(new Color(0f, 0f, 0f, 0f));
	jSlider.addChangeListener(this);
	
	jSpinner = new JSpinner();
	jSpinner.setFocusable(true);
	jSpinner.setOpaque(false);
	//            jSpinner.setBackground(new Color(0f, 0f, 0f, 0f));
	jSpinner.setFont(new java.awt.Font("Default", 0, 10));
	jSpinner.setBorder(mLineBorder);
	jSpinner.getEditor().setOpaque(false);
	//            jSpinner.getEditor().setBackground(new Color(0f, 0f, 0f, 0f));
	// jSpinner.getTextField().setOpaque(false);
	((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setOpaque(false);
	//            ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setBackground(new Color(0f, 0f, 0f, 0f));
	jSpinner.addChangeListener(this);
	jSpinner.addKeyListener(this);
    }
    
    
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int selectedViewRow, int selectedViewCol){
	mSortedTableModel = ((MSortedTableModel)mColoredJTable.getModel());
	selectedModelRow = mSortedTableModel.getRowViewToModelIndex( selectedViewRow );
	selectedModelCol = selectedViewCol;
	selectedState = mSortedTableModel.getRowState( selectedModelRow );
	
	if(value instanceof ComboBoxModel){
	    selectedValue  = ((ComboBoxModel)value).getSelectedItem();
	    editedComponent = jComboBox;
	    ((JComboBox)editedComponent).setModel((ComboBoxModel) value);
	}
	else if(value instanceof SpinnerNumberModel){
	    selectedValue  = (Integer) ((SpinnerNumberModel)value).getValue();
	    editedComponent = jSpinner;
	    ((JSpinner)editedComponent).setModel((SpinnerNumberModel) value);
	    ((JSpinner.NumberEditor)jSpinner.getEditor()).getTextField().selectAll();
	}
	else if(value instanceof Boolean){
	    selectedValue = (Boolean) value;
	    editedComponent = jCheckBox;
	    ((JCheckBox)editedComponent).setSelected((Boolean) value );
	}
	else if(value instanceof String){
	    selectedValue = ((String)value).trim();
	    editedComponent = jTextField;
	    ((JTextField)editedComponent).setText((String) value );
	}
	else if(value instanceof MPasswordField){
	    editedComponent = mPasswordField;
	    ((MPasswordField)editedComponent).setGeneratesChangeEvent( ((MPasswordField)value).getGeneratesChangeEvent() );
	    char[] password = ((MPasswordField)value).getPassword();
	    selectedValue = new String(password);
	    ((MPasswordField)editedComponent).setText( new String(password) );
	}
	else if( (value instanceof Integer) 
		 && (selectedModelCol == mSortedTableModel.getOrderModelIndex()) ){
	    selectedValue  = value;
	    editedComponent = jSpinner;
	    ((JSpinner)editedComponent).setModel( new SpinnerNumberModel( ((Integer)value).intValue(), 0, mSortedTableModel.getRowCount()+1, 1 ));
	    ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().selectAll();
	}
	else{
	    selectedValue = value;
	    editedComponent = new JTextField();
	    editedComponent.setFont(font);
	    ((JTextField)editedComponent).setHorizontalAlignment(JTextField.CENTER);
	    editedComponent.setOpaque(true);
	    ((JTextField)editedComponent).setText("UNSUPPORTED EDITOR for: " + value.getClass());
	}
	return editedComponent;
    }
    
    private void updateValues(){
	if(editedComponent instanceof JComboBox){
	    returnValue = ((JComboBox)editedComponent).getModel();
	    newValue = ((ComboBoxModel)returnValue).getSelectedItem();
	}
	else if(editedComponent instanceof JCheckBox){
	    returnValue = (Boolean) ((JCheckBox)editedComponent).isSelected();
	    newValue = returnValue;
	}
	else if(editedComponent instanceof JSpinner){
	    if( selectedModelCol == mSortedTableModel.getOrderModelIndex() ){
		returnValue = (Integer) ((JSpinner)editedComponent).getModel().getValue();
		newValue = returnValue;
	    }
	    else{
		returnValue = ((JSpinner)editedComponent).getModel();
		newValue = (Integer) ((SpinnerNumberModel)returnValue).getValue();
	    }
	}
	else if(editedComponent instanceof MPasswordField){
	    returnValue = new MPasswordField();
	    ((MPasswordField)returnValue).setGeneratesChangeEvent( ((MPasswordField)editedComponent).getGeneratesChangeEvent() );
	    ((MPasswordField)returnValue).setText( new String(((MPasswordField)editedComponent).getPassword()) );
	    newValue = new String(((MPasswordField)returnValue).getPassword());
	}
	else if(editedComponent instanceof JTextField){
	    returnValue = ((JTextField)editedComponent).getText().trim();
	    newValue = returnValue;
	}
	else{
	    returnValue = "UNKNOWN EDITOR USED";
	    newValue = returnValue;
	}
    }
    
    private void showStatusChange(){
	if( selectedValue.equals(newValue) ){
	    System.err.println("row UNCHANGED: " + newValue.toString() );
	    mSortedTableModel.setRowState(selectedState, selectedModelRow);
	}
	else{
	    System.err.println("row CHANGED from: " + selectedValue.toString() + " to: " + newValue.toString());
	    if(editedComponent instanceof MPasswordField){
		if( ((MPasswordField)editedComponent).getGeneratesChangeEvent() ){
		    mSortedTableModel.setRowChanged(selectedModelRow);
		}
	    }
	    else{
		mSortedTableModel.setRowChanged(selectedModelRow);
	    }
	}            
    }

    public Object getCellEditorValue(){
	updateValues();
	showStatusChange();
	return returnValue;
    }
    
    // for check boxes, and combo boxes
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
	getCellEditorValue();
	//mColoredJTable.changeSelection(selectedRow, selectedCol, false, false);
    }        
    
    // for the sliders, spinners
    public void stateChanged(javax.swing.event.ChangeEvent changeEvent) {
	getCellEditorValue();
	//mColoredJTable.changeSelection(selectedModelRow, selectedModelCol, false, false);
    }

    // for spinner
    public void keyPressed(KeyEvent e){}
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){
	getCellEditorValue();
	//mColoredJTable.changeSelection(selectedModelRow, selectedModelCol, false, false);
    }
    
    // for text fields and password fields        
    public void caretUpdate(javax.swing.event.CaretEvent caretEvent) {
	getCellEditorValue();
	//((MSortedTableModel)mColoredJTable.getModel()).setRowChanged(selectedRow);
    }
    
}
