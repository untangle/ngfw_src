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

package com.untangle.gui.util;

import javax.swing.*;

import com.untangle.gui.node.MNodeJButton;
import com.untangle.uvm.*;
import com.untangle.uvm.toolbox.DownloadComplete;
import com.untangle.uvm.toolbox.DownloadProgress;
import com.untangle.uvm.toolbox.DownloadSummary;
import com.untangle.uvm.toolbox.InstallComplete;
import com.untangle.uvm.toolbox.InstallTimeout;
import com.untangle.uvm.toolbox.ProgressVisitor;

public class Visitor implements ProgressVisitor{
    private boolean isDone = false;
    private boolean isSuccessful = false;
    private int fileCountTotal = 0;
    private int byteCountTotal = 0;
    private int currentFileIndex = 0;
    private int currentByteIndex = 0;
    private int currentByteIncrement = 0;
    private Object visualizer;
    private JProgressBar progressBar;
    private MNodeJButton mNodeJButton;
    private boolean isProgressBar;
    // public methods ----------------------------------------------------

    public Visitor(JProgressBar progressBar){
        visualizer = progressBar;
        isProgressBar = true;
    }

    public Visitor(MNodeJButton mNodeJButton){
        visualizer = mNodeJButton;
        isProgressBar = false;
    }

    public boolean isDone(){
        return isDone;
    }

    public boolean isSuccessful(){
        return isSuccessful;
    }

    // ProgressVisitor methods -------------------------------------------

    public void visitDownloadSummary(final DownloadSummary ds){
        fileCountTotal = ds.getCount();
        byteCountTotal = ds.getSize();
    }

    public void visitDownloadProgress(final DownloadProgress dp){
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            currentByteIncrement = dp.getSize();
            String progressString;
            float currentPercentComplete = ((float)(currentByteIndex + dp.getBytesDownloaded())) / ((float)byteCountTotal);
            int progressIndex = (int) (90f*currentPercentComplete);
            if(isProgressBar){
                progressString = "Downloading file " + (currentFileIndex+1) + " of " + fileCountTotal
                    + " (" + byteCountTotal/1000 + "KBytes " + "@ "  + dp.getSpeed() + ")";
                ((JProgressBar)visualizer).setString( progressString );
                ((JProgressBar)visualizer).setValue(progressIndex );
            }
            else{
                progressString = "Get @ "  + dp.getSpeed();
                ((MNodeJButton)visualizer).setProgress(progressString, progressIndex );
            }
        }});
    }

    public void visitDownloadComplete(final DownloadComplete dc){
        if(!dc.getSuccess()){
            isSuccessful = false;
            isDone = true;
        }
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            currentByteIndex += currentByteIncrement;
            currentFileIndex++;
            if(!dc.getSuccess()){
                if(isProgressBar){
                    ((JProgressBar)visualizer).setString( "Download failed.  Please try again." );
                    ((JProgressBar)visualizer).setValue(100);
                }
                else{
                    ((MNodeJButton)visualizer).setProgress("Failed", 100);
                }
            }
        }});
    }

    public void visitInstallComplete(final InstallComplete ic){
        isSuccessful = ic.getSuccess();
        isDone = true;
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            if(ic.getSuccess()){
                if(isProgressBar){
                    ((JProgressBar)visualizer).setString( "Download successful." );
                    ((JProgressBar)visualizer).setValue(100);
                }
                else{
                    ((MNodeJButton)visualizer).setProgress("Download complete", 100);
                    ((MNodeJButton)visualizer).setEnabled(false);
                }
            }
            else{
                if(isProgressBar){
                    ((JProgressBar)visualizer).setString( "Download failed.  Please try again." );
                    ((JProgressBar)visualizer).setValue(100);
                }
                else{
                    ((MNodeJButton)visualizer).setProgress("Failed", 100);
                    ((MNodeJButton)visualizer).setEnabled(true);
                }
            }
        }});
    }

    public void visitInstallTimeout(final InstallTimeout it){
        isSuccessful = false;
        isDone = true;
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            if(isProgressBar){
                ((JProgressBar)visualizer).setString( "Download timed out.  Please try again." );
                ((JProgressBar)visualizer).setValue(100);
            }
            else{
                ((MNodeJButton)visualizer).setProgress("Failed", 100);
            }
        }});
    }
}
