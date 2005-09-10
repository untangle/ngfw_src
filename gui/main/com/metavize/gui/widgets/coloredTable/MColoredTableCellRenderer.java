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

import com.metavize.gui.widgets.editTable.MSortedTableModel;
import com.metavize.gui.util.Util;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.Date;

import com.metavize.gui.util.MPasswordField;
 
 
 
public class MColoredTableCellRenderer extends DefaultTableCellRenderer {
    
    
        private static ImageIcon addImageIcon;
        private static ImageIcon changedImageIcon;
        private static ImageIcon removeImageIcon;
        private static ImageIcon savedImageIcon;
    
    /*
        private static final ImageIcon addImageIcon = new ImageIcon( Thread.currentThread().getContextClassLoader().getResource("/com/metavize/gui/widgets/coloredTable/IconAdd13x13.png") );
        private static final ImageIcon changedImageIcon = new ImageIcon( Thread.currentThread().getContextClassLoader().getResource("/com/metavize/gui/widgets/coloredTable/IconChanged15x15.png") );
        private static final ImageIcon removeImageIcon = new ImageIcon( Thread.currentThread().getContextClassLoader().getResource("/com/metavize/gui/widgets/coloredTable/IconRemove13x13.png") );
        private static final ImageIcon savedImageIcon = new ImageIcon( Thread.currentThread().getContextClassLoader().getResource("/com/metavize/gui/widgets/coloredTable/IconSaved13x13.png") );
    */
    private static final Color unselectedOddColor = new Color(193, 193, 206);
    private static final Color unselectedEvenColor = new Color(183, 183, 196);
    private static final Color uneditableOddColor = unselectedOddColor; //new Color(216, 193, 193);
    private static final Color uneditableEvenColor = unselectedEvenColor; //new Color(206, 183, 183);
    private static final MLineBorder unselectedOddBorder = new MLineBorder(unselectedOddColor, 2);
    private static final MLineBorder unselectedEvenBorder = new MLineBorder(unselectedEvenColor, 2);
    private static final MLineBorder uneditableOddBorder = unselectedOddBorder; //new MLineBorder(uneditableOddColor, 2);
    private static final MLineBorder uneditableEvenBorder = unselectedEvenBorder; //new MLineBorder(uneditableEvenColor, 2);
    
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
                
        private static JLabel renderJLabel = new JLabel();
        private static JTextField renderJTextField = new JTextField();
        private static JComboBox renderJComboBox = new JComboBox();
        private static JCheckBox renderJCheckBox = new JCheckBox();
        private static JSlider renderJSlider = new JSlider();
        private static JSpinner renderJSpinner = new JSpinner();
        private static MPasswordField renderMPasswordField = new MPasswordField();
        

        public MColoredTableCellRenderer(){
            
            if(addImageIcon == null)
                addImageIcon = new ImageIcon( this.getClass().getResource("/com/metavize/gui/widgets/coloredTable/IconAdd13x13.png") );
            if(changedImageIcon == null)
                changedImageIcon = new ImageIcon( this.getClass().getResource("/com/metavize/gui/widgets/coloredTable/IconChanged15x15.png") );
            if(removeImageIcon == null)
                removeImageIcon = new ImageIcon( this.getClass().getResource("/com/metavize/gui/widgets/coloredTable/IconRemove13x13.png") );
            if(savedImageIcon == null)
                savedImageIcon = new ImageIcon( this.getClass().getResource("/com/metavize/gui/widgets/coloredTable/IconSaved13x13.png") );
            
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
            
            // CONTENT
            if(value instanceof ComboBoxModel){
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
		     || (value instanceof Double) ){
                if(value != null)
                    renderJLabel.setText(value.toString());
                else
                    renderJLabel.setText("");
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
                }
		else if( (col == 1) && (value instanceof Integer) ){
		    renderJLabel.setHorizontalAlignment(JTextField.CENTER);
                    renderJLabel.setIcon(null);
		}
                else{
                    renderJLabel.setHorizontalAlignment(JTextField.LEFT);
                    renderJLabel.setIcon(null);
                }
                renderJComponent = renderJLabel;
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
            else if(value instanceof SpinnerNumberModel){
                renderJSpinner.setModel( (SpinnerNumberModel) value );
                renderJComponent = renderJSpinner;
            }
	    else if(value instanceof Date){
		renderJLabel.setIcon(null);
		renderJLabel.setHorizontalAlignment(JTextField.LEFT);
		renderJLabel.setText( Util.getLogDateFormat().format((Date)value) );
		renderJComponent = renderJLabel;
	    }
            else{
                if(value != null)
                    renderJLabel.setText("UNSUPPORTED RENDER for: " + value.getClass());
                else
                    renderJLabel.setText("UNSUPPORTED RENDER for: null" );
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
            if(hasFocus && isSelected){
                if(isEditable){
                    renderJComponent.setBackground(focusBackgroundEditableColor);
                    renderJComponent.setBorder(mLineHighlightedUnselectedEditableBorder);
                }
                else{
                    renderJComponent.setBackground(focusBackgroundUneditableColor);
                    renderJComponent.setBorder(mLineHighlightedUnselectedUneditableBorder);
                }
                
            }
            else if( !hasFocus && isSelected){
                if(isEditable){
                    renderJComponent.setBackground(selectedBackgroundEditableColor);
                    renderJComponent.setBorder(mLineHighlightedSelectedEditableBorder);
                }
                else{
                    renderJComponent.setBackground(selectedBackgroundUneditableColor);
                    renderJComponent.setBorder(mLineHighlightedSelectedUneditableBorder);
                }
            }
            else{
                if(isEditable){
                    if(row % 2 == 0){
                        renderJComponent.setBackground(unselectedEvenColor);
                        renderJComponent.setBorder(unselectedEvenBorder);
                    }
                    else{
                        renderJComponent.setBackground(unselectedOddColor);
                        renderJComponent.setBorder(unselectedOddBorder);
                    }
                }
                else{
                    if(row % 2 == 0){
                        renderJComponent.setBackground(uneditableEvenColor);
                        renderJComponent.setBorder(uneditableEvenBorder);
                    }
                    else{
                        renderJComponent.setBackground(uneditableOddColor);
                        renderJComponent.setBorder(uneditableOddBorder);
                    }
                }
            }

            return renderJComponent;
        }
        
    }


