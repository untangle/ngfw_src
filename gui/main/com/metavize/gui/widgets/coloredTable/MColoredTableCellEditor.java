/*
 * MTableCellEditor.java
 *
 * Created on July 6, 2004, 7:38 AM
 */

package com.metavize.gui.widgets.coloredTable;

import com.metavize.gui.util.*;
import com.metavize.gui.widgets.editTable.*;

import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;



/**
 *
 * @author  inieves
 */
public class MColoredTableCellEditor extends DefaultCellEditor implements KeyListener, ActionListener, ChangeListener, CaretListener {
        
    private MColoredJTable mColoredJTable;
            
    private int selectedRow, selectedCol;
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
            jSpinner.setOpaque(true);
            jSpinner.setBackground(new Color(0f, 0f, 0f, 0f));
            jSpinner.setFont(new java.awt.Font("Default", 0, 10));
            jSpinner.setBorder(mLineBorder);
            ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setOpaque(true);
            ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setBackground(new Color(0f, 0f, 0f, 0f));
            jSpinner.addChangeListener(this);
            jSpinner.addKeyListener(this);
        }

    
        
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col){
            selectedRow = row;
            selectedCol = col;

	    //	    System.err.println("GETTING EDITOR AT ROW: " + row);

            selectedState = ((MSortedTableModel)mColoredJTable.getModel()).getRowState(row);
            if(value instanceof ComboBoxModel){
                selectedValue  = ((ComboBoxModel)value).getSelectedItem();
                editedComponent = jComboBox;
                ((JComboBox)editedComponent).setModel((ComboBoxModel) value);
            }
            else if(value instanceof SpinnerNumberModel){
                selectedValue  = new Integer( ((Integer)((SpinnerNumberModel)value).getValue()).intValue() );
                editedComponent = jSpinner;
                ((JSpinner)editedComponent).setModel((SpinnerNumberModel) value);
            }
            else if(value instanceof Boolean){
                selectedValue = new Boolean( ((Boolean) value).booleanValue() );
                editedComponent = jCheckBox;
                ((JCheckBox)editedComponent).setSelected( ((Boolean) value).booleanValue() );
            }
            else if(value instanceof String){
                selectedValue = value;
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
        
        
        public Object getCellEditorValue(){
            if(editedComponent instanceof JComboBox){
                returnValue = ((JComboBox)editedComponent).getModel();
                newValue = ((ComboBoxModel)returnValue).getSelectedItem();
            }
            else if(editedComponent instanceof JCheckBox){
                returnValue = new Boolean(((JCheckBox)editedComponent).isSelected());
                newValue = returnValue;
            }
            else if(editedComponent instanceof JSpinner){
                returnValue = ((JSpinner)editedComponent).getModel();
                newValue = new Integer( ((Integer) ((SpinnerNumberModel)returnValue).getValue()).intValue() );
            }
            else if(editedComponent instanceof MPasswordField){
                returnValue = new MPasswordField();
		((MPasswordField)returnValue).setGeneratesChangeEvent( ((MPasswordField)editedComponent).getGeneratesChangeEvent() );
                ((MPasswordField)returnValue).setText( new String(((MPasswordField)editedComponent).getPassword()) );
                newValue = new String(((MPasswordField)returnValue).getPassword());
            }
            else if(editedComponent instanceof JTextField){
                returnValue = ((JTextField)editedComponent).getText();
                newValue = returnValue;
            }
            else{
                returnValue = "UNKNOWN EDITOR USED";
                newValue = returnValue;
            }
            
            if( selectedValue.equals(newValue) ){
                //System.err.println("unchanged:" + newValue.toString() +": from:" + selectedValue.toString() + ":");
                ((MSortedTableModel)mColoredJTable.getModel()).setRowState(selectedState, selectedRow);
		System.err.println("A");
            }
            else{
		if(editedComponent instanceof MPasswordField){
		    if( ((MPasswordField)editedComponent).getGeneratesChangeEvent() ){
					System.err.println("B");
			((MSortedTableModel)mColoredJTable.getModel()).setRowChanged(selectedRow);
		    }
		}
		else{
		    		System.err.println("C");
		    ((MSortedTableModel)mColoredJTable.getModel()).setRowChanged(selectedRow);
		}
                //System.err.println("changed:" + newValue.toString() +": from:" + selectedValue.toString() + ":");
			
            }
            
            return returnValue;
        }
           
        // for check boxes, and combo boxes
        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            getCellEditorValue();
            //this.stopCellEditing();
            //mColoredJTable.changeSelection(selectedRow, selectedCol, false, false);
        }        
        
        // for the sliders
        public void stateChanged(javax.swing.event.ChangeEvent changeEvent) {
            //if(!jSpinner.get  jSlider.getValueIsAdjusting()){
                getCellEditorValue();
                this.stopCellEditing();
                mColoredJTable.changeSelection(selectedRow, selectedCol, false, false);
            //}
        }
        
        public void keyPressed(KeyEvent e){}
        public void keyReleased(KeyEvent e){}
        public void keyTyped(KeyEvent e){
            getCellEditorValue();
            mColoredJTable.changeSelection(selectedRow, selectedCol, false, false);
        }
        
        // for text fields and password fields        
        public void caretUpdate(javax.swing.event.CaretEvent caretEvent) {
            //System.err.println("updating");
            getCellEditorValue();
            //((MSortedTableModel)mColoredJTable.getModel()).setRowChanged(selectedRow);
        }
        
}
