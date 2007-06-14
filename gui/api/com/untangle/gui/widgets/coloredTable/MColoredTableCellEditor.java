/*
 * $HeadURL:$
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
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;

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
    private final JSpinner jDateSpinner;
    private final MPasswordField mPasswordField;
    private final JButton jButton;
    private final JPanel jButtonJPanel;

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

        jButton = new JButton();
        jButton.setOpaque(false);
        jButton.setFont(new java.awt.Font("Default", 0, 12));
        jButton.addActionListener(this);
        jButtonJPanel = new JPanel();
        jButtonJPanel.setOpaque(false);
        jButtonJPanel.setLayout(new BorderLayout());
        jButtonJPanel.setBorder(new EmptyBorder(2,2,2,2));
        jButtonJPanel.add(jButton);

        jCheckBox = new JCheckBox();
        jCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
        jCheckBox.setOpaque(true);
        jCheckBox.setBorderPainted(true);
        jCheckBox.setBorder(mLineBorder);
        jCheckBox.setBackground(focusBackgroundColor);
        jCheckBox.addActionListener(this);

        jComboBox = new JComboBox();
        jComboBox.setFont(font);
        jComboBox.setOpaque(true);
        jComboBox.setBorder(mLineBorder);
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
        jSpinner.getEditor().setOpaque(false);
        ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setOpaque(true);
        ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().setBackground(new Color(0f,0f,0f,0f));
        jSpinner.addChangeListener(this);

        jDateSpinner = new JSpinner( new SpinnerDateModel((new GregorianCalendar()).getTime(), null, null, Calendar.MINUTE) );
        jDateSpinner.setOpaque(false);
        jDateSpinner.setFont(new java.awt.Font("Default", 0, 10));
        jDateSpinner.setBorder(mLineBorder);
        jDateSpinner.getEditor().setOpaque(false);
        ((JSpinner.DefaultEditor)jDateSpinner.getEditor()).getTextField().setOpaque(false);
        jDateSpinner.addChangeListener(this);
        JFormattedTextField tf = ((JSpinner.DefaultEditor)jDateSpinner.getEditor()).getTextField();
        DefaultFormatterFactory factory = (DefaultFormatterFactory)tf.getFormatterFactory();
        DateFormatter formatter = (DateFormatter)factory.getDefaultFormatter();
        formatter.setFormat(new SimpleDateFormat("HH:mm " + "(" + "a" + ")"));
    }



    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int selectedViewRow, int selectedViewCol){
        mSortedTableModel = ((MSortedTableModel)mColoredJTable.getModel());
        selectedModelRow = mSortedTableModel.getRowViewToModelIndex( selectedViewRow );
        selectedModelCol = selectedViewCol;
        if( !mSortedTableModel.getAlwaysSelectable() )
            selectedState = mSortedTableModel.getRowState( selectedModelRow );

        if( value instanceof ButtonRunnable ){
            selectedValue = jButton;
            editedComponent = jButton;
            returnValue = value;
            jButton.setText( ((ButtonRunnable)value).getButtonText() );
            jButton.setEnabled( ((ButtonRunnable)value).isEnabled() );
            ((ButtonRunnable)value).setTopLevelWindow( (Window) mColoredJTable.getTopLevelAncestor() );
            ((ButtonRunnable)value).setCellEditor( this );
            ActionListener[] actionListeners = jButton.getActionListeners();
            for( ActionListener actionListener : actionListeners )
                jButton.removeActionListener(actionListener);
            jButton.addActionListener( (ButtonRunnable) value );
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                jButton.requestFocusInWindow();
            }});
            return jButtonJPanel;
        }
        else if(value instanceof ComboBoxModel){
            selectedValue  = ((ComboBoxModel)value).getSelectedItem();
            editedComponent = jComboBox;
            ((JComboBox)editedComponent).setModel((ComboBoxModel) value);
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                jComboBox.requestFocusInWindow();
            }});
        }
        else if(value instanceof AbstractSpinnerModel){
            if(value instanceof SpinnerDateModel){
                selectedValue  = ((AbstractSpinnerModel)value).getValue();
                editedComponent = jDateSpinner;
                Calendar calendar = new GregorianCalendar();
                calendar.setTime( ((SpinnerDateModel)value).getDate() );
                ((JSpinner)editedComponent).setValue( calendar.getTime() );
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().requestFocusInWindow();
                    ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().selectAll();
                }});
            }
            else{
                selectedValue  = ((AbstractSpinnerModel)value).getValue();
                editedComponent = jSpinner;
                ((JSpinner)editedComponent).setModel((AbstractSpinnerModel) value);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().requestFocusInWindow();
                    ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().selectAll();
                }});
            }
        }
        else if(value instanceof Boolean){
            selectedValue = (Boolean) value;
            editedComponent = jCheckBox;
            ((JCheckBox)editedComponent).setSelected((Boolean) value );
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                jCheckBox.requestFocusInWindow();
            }});
        }
        else if(value instanceof String){
            selectedValue = ((String)value).trim();
            editedComponent = jTextField;
            ((JTextField)editedComponent).setText((String) value );
        }
        else if( (value instanceof IPMaddrString)
                 || (value instanceof IPaddrString)
                 || (value instanceof IPPortString) ){
            selectedValue = value;
            editedComponent = jTextField;
            ((JTextField)editedComponent).setText( selectedValue.toString() );
        }
        else if(value instanceof Date){
            selectedValue = ((Date)value).toString().trim();
            editedComponent = jTextField;
            ((JTextField)editedComponent).setText( Util.getLogDateFormat().format((Date)value) );
        }
        else if(value instanceof MPasswordField){
            editedComponent = mPasswordField;
            ((MPasswordField)editedComponent).setGeneratesChangeEvent( ((MPasswordField)value).getGeneratesChangeEvent() );
            char[] password = ((MPasswordField)value).getPassword();
            selectedValue = new String(password);
            ((MPasswordField)editedComponent).setText( new String(password) );
        }
        else if( (value instanceof Integer)
                 && (selectedModelCol == mSortedTableModel.getOrderViewIndex()) ){
            selectedValue  = value;
            editedComponent = jSpinner;
            ((JSpinner)editedComponent).setModel( new SpinnerNumberModel( ((Integer)value).intValue(),
                                                                          1, mSortedTableModel.getRowCount(), 1 ));
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().requestFocusInWindow();
                ((JSpinner.DefaultEditor)jSpinner.getEditor()).getTextField().selectAll();
            }});
        }
        else{
            selectedValue = value;
            editedComponent = new JTextField();
            editedComponent.setFont(font);
            ((JTextField)editedComponent).setHorizontalAlignment(JTextField.CENTER);
            editedComponent.setOpaque(true);
            ((JTextField)editedComponent).setText("UNSUPPORTED EDITOR for: " + value.getClass());
        }

        jTextField.setEditable(mSortedTableModel.getAlwaysEditable());

        // AUTO HIGHLIGHT
        if( editedComponent == jTextField ){
            final JTextField selectedJTextField = jTextField;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                selectedJTextField.requestFocusInWindow();
                selectedJTextField.selectAll();
            }});
        }
        else if( editedComponent == mPasswordField ){
            final MPasswordField selectedMPasswordField = mPasswordField;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                selectedMPasswordField.requestFocusInWindow();
                selectedMPasswordField.selectAll();
            }});
        }

        return editedComponent;
    }

    private void updateValues(){
        if(editedComponent instanceof JButton){
            newValue = selectedValue;
        }
        else if(editedComponent instanceof JComboBox){
            returnValue = ((JComboBox)editedComponent).getModel();
            newValue = ((ComboBoxModel)returnValue).getSelectedItem();
        }
        else if(editedComponent instanceof JCheckBox){
            returnValue = (Boolean) ((JCheckBox)editedComponent).isSelected();
            newValue = returnValue;
        }
        else if(editedComponent instanceof JSpinner){
            if( selectedModelCol == mSortedTableModel.getOrderViewIndex() ){
                returnValue = (Integer) ((JSpinner)editedComponent).getModel().getValue();
                newValue = returnValue;
            }
            else{
                returnValue = ((JSpinner)editedComponent).getModel();
                newValue = ((AbstractSpinnerModel)returnValue).getValue();
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

    private void showStatusChange(boolean isFinalChange){
        if( mSortedTableModel.getAlwaysSelectable() )
            return;
        if( (selectedValue instanceof IPMaddrString)
            || (selectedValue instanceof IPaddrString)
            || (selectedValue instanceof IPPortString) ){
            if( !selectedValue.toString().equals(((String)returnValue).trim()) )
                mSortedTableModel.setRowChanged(selectedModelRow, isFinalChange);
            else
                mSortedTableModel.setRowState(selectedState, selectedModelRow);
        }
        else if( selectedValue.equals(newValue) ){
            if( (returnValue instanceof ButtonRunnable) && ( ((ButtonRunnable)returnValue).valueChanged()) ){
                mSortedTableModel.setRowChanged(selectedModelRow, isFinalChange);
            }
            else
                mSortedTableModel.setRowState(selectedState, selectedModelRow);
        }
        else{
            if(editedComponent instanceof MPasswordField){
                if( ((MPasswordField)editedComponent).getGeneratesChangeEvent() ){
                    mSortedTableModel.setRowChanged(selectedModelRow, isFinalChange);
                }
            }
            else{
                mSortedTableModel.setRowChanged(selectedModelRow, isFinalChange);
            }
        }
        mSortedTableModel.handleDependencies(selectedModelCol, selectedModelRow);
    }

    public Object getCellEditorValue(){
        updateValues();
        showStatusChange(true);

        if( mSortedTableModel.getAlwaysSelectable() )
            return selectedValue;
        else{
            if( selectedValue instanceof IPMaddrString ){
                ((IPMaddrString)selectedValue).setString((String)returnValue);
                return selectedValue;
            }
            else if( selectedValue instanceof IPaddrString ){
                ((IPaddrString)selectedValue).setString((String)returnValue);
                return selectedValue;
            }
            else
                return returnValue;
        }

    }

    private void update(boolean isFinal){
        updateValues();
        showStatusChange(isFinal);
    }

    // for check boxes, and combo boxes
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        update(true);
    }

    // for the sliders, spinners
    public void stateChanged(javax.swing.event.ChangeEvent changeEvent) {
        update(false);
    }

    // for spinner
    public void keyPressed(KeyEvent e){}
    public void keyReleased(KeyEvent e){}
    public void keyTyped(KeyEvent e){
        update(false);
    }

    // for text fields and password fields
    public void caretUpdate(javax.swing.event.CaretEvent caretEvent) {
        update(false);
    }

}
