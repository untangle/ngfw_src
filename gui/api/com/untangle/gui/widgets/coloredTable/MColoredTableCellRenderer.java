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

package com.untangle.gui.widgets.coloredTable;

import com.untangle.gui.widgets.editTable.MSortedTableModel;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.util.*;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.*;
import javax.swing.text.*;
import java.text.*;

 
 
 
public class MColoredTableCellRenderer extends DefaultTableCellRenderer {


    private static Date activeDate = new Date(0l);
    private static Date leaseDate = new Date(1l);
    
    private static ImageIcon addImageIcon;
    private static ImageIcon changedImageIcon;
    private static ImageIcon removeImageIcon;
    private static ImageIcon savedImageIcon;
        
    private static final Color unselectedOddColor = new Color(193, 193, 206);
    private static final Color unselectedEvenColor = new Color(175, 175, 196);
    private static final Color uneditableOddColor = unselectedOddColor; //new Color(216, 193, 193);
    private static final Color uneditableEvenColor = unselectedEvenColor; //new Color(206, 183, 183);
    private static final MLineBorder unselectedOddBorder = new MLineBorder(unselectedOddColor, 2);
    private static final MLineBorder unselectedEvenBorder = new MLineBorder(unselectedEvenColor, 2);
    private static final MLineBorder uneditableOddBorder = unselectedOddBorder; //new MLineBorder(uneditableOddColor, 2);
    private static final MLineBorder uneditableEvenBorder = unselectedEvenBorder; //new MLineBorder(uneditableEvenColor, 2);

    public static final Color removedOddColor = new Color(255,109,109);
    public static final Color removedEvenColor = new Color(255,81,81);
    public static final Color addedOddColor = new Color(105,255,123);
    public static final Color addedEvenColor = new Color(33,255,58);
    public static final Color changedOddColor = new Color(255,179,100);
    public static final Color changedEvenColor = new Color(255,158,57);
    private static final MLineBorder removedEvenBorder = new MLineBorder(removedEvenColor, 2);
    private static final MLineBorder removedOddBorder = new MLineBorder(removedOddColor, 2);    
    private static final MLineBorder addedEvenBorder = new MLineBorder(addedEvenColor, 2);
    private static final MLineBorder addedOddBorder = new MLineBorder(addedOddColor, 2);    
    private static final MLineBorder changedEvenBorder = new MLineBorder(changedEvenColor, 2);
    private static final MLineBorder changedOddBorder = new MLineBorder(changedOddColor, 2);    


    private static final Color unselectedColor = new Color(193, 193, 206);
    private static final Color selectedColor = new Color(193, 193, 226);
    
    private static final Color editableColor = new Color(64, 64, 236);
    private static final Color uneditableColor = new Color(236, 64, 64);
    
    private static final Color focusBackgroundEditableColor = selectedColor;
    private static final Color selectedBackgroundEditableColor = selectedColor;
    private static final Color normalBackgroundEditableColor = unselectedColor;
    private static final Color focusBackgroundUneditableColor = selectedColor;
    private static final Color selectedBackgroundUneditableColor = selectedColor;
    private static final Color normalBackgroundUneditableColor = unselectedColor;
    
    private static final Color focusBorderEditableColor = selectedColor;
    private static final Color selectedBorderEditableColor = selectedColor;
    private static final Color normalBorderEditableColor = unselectedColor;
    private static final Color focusBorderUneditableColor = selectedColor;
    private static final Color selectedBorderUneditableColor = selectedColor;
    private static final Color normalBorderUneditableColor = unselectedColor;
    
    private static final MLineBorder mLineHighlightedSelectedEditableBorder = new MLineBorder(selectedColor, 2);
    private static final MLineBorder mLineHighlightedSelectedUneditableBorder = new MLineBorder(selectedColor, 2);        
    private static final MLineBorder mLineHighlightedUnselectedEditableBorder = new MLineBorder(editableColor, 2);
    private static final MLineBorder mLineHighlightedUnselectedUneditableBorder = new MLineBorder(uneditableColor, 2);
    
    private static final MLineBorder mLineEditableNormalBorder = new MLineBorder(unselectedColor, 2);
    private static final MLineBorder mLineUneditableNormalBorder = new MLineBorder(unselectedColor, 2);        
    
    private static JButton renderJButton = new JButton();
    private static JPanel renderJButtonJPanel = new JPanel();
    private static JLabel renderJLabel = new JLabel();
    private static JTextField renderJTextField = new JTextField();
    private static JComboBox renderJComboBox = new JComboBox();
    private static JCheckBox renderJCheckBox = new JCheckBox();
    private static JSlider renderJSlider = new JSlider();
    private static JSpinner renderJSpinner = new JSpinner();
    private static JSpinner renderDateJSpinner = new JSpinner( new SpinnerDateModel((new GregorianCalendar()).getTime(), null, null, Calendar.MINUTE) );
    
    private static SpinnerNumberModel indexSpinnerNumberModel = new SpinnerNumberModel(1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    private static MPasswordField renderMPasswordField = new MPasswordField();
        

        public MColoredTableCellRenderer(){
            
            if(addImageIcon == null)
                addImageIcon = new ImageIcon( this.getClass().getResource("/com/untangle/gui/widgets/coloredTable/IconAdd13x13.png") );
            if(changedImageIcon == null)
                changedImageIcon = new ImageIcon( this.getClass().getResource("/com/untangle/gui/widgets/coloredTable/IconChanged15x15.png") );
            if(removeImageIcon == null)
                removeImageIcon = new ImageIcon( this.getClass().getResource("/com/untangle/gui/widgets/coloredTable/IconRemove13x13.png") );
            if(savedImageIcon == null)
                savedImageIcon = new ImageIcon( this.getClass().getResource("/com/untangle/gui/widgets/coloredTable/IconSaved13x13.png") );
            
	    renderJButton.setFont(new java.awt.Font("Dialog", 0, 12));
	    renderJButton.setFocusable(false);
	    renderJButton.setEnabled(true);
	    renderJButton.setOpaque(false);
	    renderJButtonJPanel = new JPanel();
	    renderJButtonJPanel.setOpaque(false);
	    renderJButtonJPanel.setLayout(new BorderLayout());
	    renderJButtonJPanel.setBorder(new EmptyBorder(2,2,2,2));
	    renderJButtonJPanel.add(renderJButton);

            renderJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            renderJLabel.setHorizontalAlignment(JTextField.LEFT);
            renderJLabel.setOpaque(true);
            renderJLabel.setFocusable(false);
            //renderJLabel.setBorder(mLineEditableBorder);
            
            renderJTextField.setFont(new java.awt.Font("Dialog", 0, 12));
            renderJTextField.setHorizontalAlignment(JTextField.LEFT);
            renderJTextField.setOpaque(true);
            renderJTextField.setFocusable(false);
            renderJTextField.setEditable(false);
            
            renderJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
            renderJComboBox.setOpaque(true);
            //renderJComboBox.setBorder(mLineEditableBorder);
            renderJComboBox.setFocusable(false);
            
            renderJCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
            renderJCheckBox.setOpaque(true);
            //renderJCheckBox.setBorder(mLineEditableBorder);
            renderJCheckBox.setBorderPainted(true);
            renderJCheckBox.setFocusable(false);
            
            renderJSlider.setOpaque(false);
            renderJSlider.setFocusable(false);
            renderJSlider.setMajorTickSpacing(25);
            renderJSlider.setMinorTickSpacing(10);
            renderJSlider.setPaintLabels(true);
            renderJSlider.setFont(new java.awt.Font("Default", 0, 10));
            renderJSlider.setBackground(new Color(0f, 0f, 0f, 0f));
            
            renderJSpinner.setOpaque(true);
            renderJSpinner.setFocusable(false);
            renderJSpinner.setFont(new java.awt.Font("Default", 0, 10));
            renderJSpinner.setBackground(new Color(0f, 0f, 0f, 0f));
            ((JSpinner.DefaultEditor)renderJSpinner.getEditor()).getTextField().setOpaque(true);
            ((JSpinner.DefaultEditor)renderJSpinner.getEditor()).getTextField().setBackground(new Color(0f, 0f, 0f, 0f));

            renderDateJSpinner.setOpaque(true);
            renderDateJSpinner.setFocusable(false);
            renderDateJSpinner.setFont(new java.awt.Font("Default", 0, 10));
            renderDateJSpinner.setBackground(new Color(0f, 0f, 0f, 0f));
            ((JSpinner.DefaultEditor)renderDateJSpinner.getEditor()).getTextField().setOpaque(true);
            ((JSpinner.DefaultEditor)renderDateJSpinner.getEditor()).getTextField().setBackground(new Color(0f, 0f, 0f, 0f));
	    JFormattedTextField tf = ((JSpinner.DefaultEditor)renderDateJSpinner.getEditor()).getTextField();
	    DefaultFormatterFactory factory = (DefaultFormatterFactory)tf.getFormatterFactory();
	    DateFormatter formatter = (DateFormatter)factory.getDefaultFormatter();
	    formatter.setFormat(new SimpleDateFormat("HH:mm " + "(" + "a" + ")"));
            
            renderMPasswordField.setFont(new java.awt.Font("Dialog", 0, 12));
            renderMPasswordField.setHorizontalAlignment(JTextField.LEFT);
            renderMPasswordField.setOpaque(true);
            renderMPasswordField.setFocusable(false);
            renderMPasswordField.setEditable(false);
        }
        
        public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int col){
            super.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row, col);
            
            boolean isEditable = jTable.isCellEditable(row, col);
            JComponent renderJComponent;  //  renderComponent
            JComponent renderSecondaryJComponent = null;
            
            String rowStatus = (String)jTable.getModel().getValueAt(row,0);

            // CONTENT
	    if(value instanceof ButtonRunnable ){
		renderJButton.setText( ((ButtonRunnable)value).getButtonText() );
		renderJButton.setEnabled( ((ButtonRunnable)value).isEnabled() );
		renderJComponent = renderJButtonJPanel;
	    }
            else if(value instanceof ComboBoxModel){
                renderJComboBox.setModel( (ComboBoxModel) value);
                renderJComponent = renderJComboBox;
            }
            else if(value instanceof Boolean){
                renderJCheckBox.setSelected(((Boolean)value).booleanValue());
                renderJComponent = renderJCheckBox;
            }
            else if( (value instanceof String)
		     || (value instanceof Integer)
		     || (value instanceof Float)
		     || (value instanceof Long)
		     || (value instanceof Double)
		     || (value instanceof IPMaddrString)
		     || (value instanceof IPaddrString)
		     || (value instanceof IPPortString)){
                if(value != null)
                    if ((value instanceof Double) || (value instanceof Float))
                        // Print all floatign point like: 0.1, 124.2, 123.0, 0.0, -2.1, -2.0
                        renderJLabel.setText(String.format("%03.1f", value));
                    else
                        renderJLabel.setText(value.toString());
                else
                    renderJLabel.setText("");

                /*
                if( col == 0 ) {
                    renderJLabel.setHorizontalAlignment(JTextField.CENTER);
                    renderJLabel.setText("");
                    if( value.equals(MSortedTableModel.ROW_ADD) ){
                        renderJLabel.setIcon( addImageIcon );
                    }
                    else if( value.equals(MSortedTableModel.ROW_CHANGED) ){
                        renderJLabel.setIcon( changedImageIcon );
                    }
                    else if( value.equals(MSortedTableModel.ROW_REMOVE) ){
                        renderJLabel.setIcon( removeImageIcon );
                    }
                    else if( value.equals(MSortedTableModel.ROW_SAVED) ){
                        renderJLabel.setIcon( savedImageIcon );
                    }
                    else{
                        renderJLabel.setHorizontalAlignment(JTextField.LEFT);
                        renderJLabel.setIcon(null);
                        renderJLabel.setText(value.toString());
                    }
                    renderJComponent = renderJLabel;
                }
                else*/
                if( (col == 0) && (value instanceof Integer) ){
                    if(isEditable){
                        renderJSpinner.setModel( indexSpinnerNumberModel );
                        renderJSpinner.setValue( value );
                        renderJComponent = renderJSpinner;
                        renderSecondaryJComponent = ((JSpinner.DefaultEditor)renderJSpinner.getEditor()).getTextField();
                    }
                    else{
                        renderJLabel.setHorizontalAlignment(JTextField.CENTER);
                        renderJLabel.setIcon(null);
                        renderJComponent = renderJLabel;
                    }
                }
                else{
                    if (value instanceof Number)
                        renderJLabel.setHorizontalAlignment(JTextField.RIGHT);
                    else
                        renderJLabel.setHorizontalAlignment(JTextField.LEFT);
                    renderJLabel.setIcon(null);
		    renderJComponent = renderJLabel;
                }
            }
            else if( value instanceof MPasswordField ){
                char[] password = ((MPasswordField)value).getPassword();
                renderMPasswordField.setText( new String(password) );
                renderJComponent = renderMPasswordField;
            }
            else if( value instanceof ImageIcon ){
                renderJLabel.setIcon( (ImageIcon) value);
                renderJLabel.setText(null);
                renderJLabel.setHorizontalAlignment(JTextField.LEFT);
                renderJComponent = renderJLabel;
            }
            else if(value instanceof AbstractSpinnerModel){
		if( value instanceof SpinnerDateModel ){
		    Calendar calendar = new GregorianCalendar();
		    calendar.setTime(((SpinnerDateModel)value).getDate());
		    renderDateJSpinner.setValue( calendar.getTime() );
		    renderJComponent = renderDateJSpinner;
		}
		else{
		    renderJSpinner.setModel( (AbstractSpinnerModel) value );
		    renderJComponent = renderJSpinner;
		}
            }
	    else if(value instanceof Date){
		Date tempDate = (Date) value;
		renderJLabel.setIcon(null);
		renderJLabel.setHorizontalAlignment(JTextField.LEFT);
		if( tempDate.equals(activeDate) )
		    renderJLabel.setText("");
        else if(tempDate.equals(leaseDate) )
            renderJLabel.setText("[not connected]");
		else{
		    renderJLabel.setText( Util.getLogDateFormat().format(tempDate) );
		}
		renderJComponent = renderJLabel;

	    }
            else{
                if(value != null)
                    renderJLabel.setText("UNSUPPORTED RENDERER for: " + value.getClass());
                else
                    renderJLabel.setText("UNSUPPORTED RENDERER for: null");
                renderJLabel.setIcon(null);
                renderJComponent = renderJLabel;
            }
            
            // FONT COLOR
            /*
            if(jTable.getModel().isCellEditable(row, col)){
                renderJComponent.setForeground(Color.BLACK);
            }
            else{
                renderJComponent.setForeground(uneditableColor);
            }*/
            
            // BORDER & BACKGROUND
	    Color backgroundColor;
	    MLineBorder borderColor;
            if(hasFocus && isSelected){
                if(isEditable){
		    backgroundColor = focusBackgroundEditableColor;
                    borderColor = mLineHighlightedUnselectedEditableBorder;
                }
                else{
                    backgroundColor = focusBackgroundUneditableColor;
                    borderColor = mLineHighlightedUnselectedUneditableBorder;
                }
                
            }
            else if(!hasFocus && isSelected){
                if(isEditable){
                    backgroundColor = selectedBackgroundEditableColor;
                    borderColor = mLineHighlightedSelectedEditableBorder;
                }
                else{
                    backgroundColor = selectedBackgroundUneditableColor;
                    borderColor = mLineHighlightedSelectedUneditableBorder;
                }
            }
            else{
                if(isEditable){
                    if(row % 2 == 0){
                        backgroundColor = unselectedEvenColor;
                        borderColor = unselectedEvenBorder;
                    }
                    else{
                        backgroundColor = unselectedOddColor;
                        borderColor = unselectedOddBorder;
                    }
                }
                else{
                    if(row % 2 == 0){
                        backgroundColor = uneditableEvenColor;
                        borderColor = uneditableEvenBorder;
                    }
                    else{
                        backgroundColor = uneditableOddColor;
                        borderColor = uneditableOddBorder;
                    }
                }
            }

            if(rowStatus.equals(MSortedTableModel.ROW_ADD)){
                if(row % 2 == 0){
                    backgroundColor = addedEvenColor;
                    borderColor = addedEvenBorder;
                }
                else{
                    backgroundColor = addedOddColor;
                    borderColor = addedOddBorder;
                }
            }
            else if(rowStatus.equals(MSortedTableModel.ROW_CHANGED)){
                if(row % 2 == 0){
                    backgroundColor = changedEvenColor;
                    borderColor = changedEvenBorder;
                }
                else{
                    backgroundColor = changedOddColor;
                    borderColor = changedOddBorder;
                }
            }
            else if(rowStatus.equals(MSortedTableModel.ROW_REMOVE)){
                if(row % 2 == 0){
                    backgroundColor = removedEvenColor;
                    borderColor = removedEvenBorder;
                }
                else{
                    backgroundColor = removedOddColor;
                    borderColor = removedOddBorder;
                }
            }

	    if( renderJComponent == renderJButton ){
		//renderJComponent.setBorder( new CompoundBorder(borderColor, ((CompoundBorder)renderJButton.getBorder()).getInsideBorder()) );
	    }
	    else{
		renderJComponent.setBackground(backgroundColor);
		renderJComponent.setBorder(borderColor);
	    }

	    if( renderSecondaryJComponent != null )
		renderSecondaryJComponent.setBackground(backgroundColor);
            return renderJComponent;
        }
        
    }


