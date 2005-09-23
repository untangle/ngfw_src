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

package com.metavize.gui.util;

import javax.swing.*;

import com.metavize.mvvm.*;


public class Visitor implements ProgressVisitor{
    private boolean done = false;
    private boolean successful = false;
    private int fileCountTotal = 0;
    private int byteCountTotal = 0;
    private int currentFileIndex = 0;
    private int currentByteIndex = 0;
    private int currentByteIncrement = 0;
    private JProgressBar progressBar;
    // public methods ----------------------------------------------------

    public Visitor(JProgressBar progressBar){
	this.progressBar = progressBar;
    }
    
    public boolean isDone(){
	return done;
    }
    
    public boolean isSuccessful(){
	return successful;
    }

    // ProgressVisitor methods -------------------------------------------

    public void visitDownloadSummary(final DownloadSummary ds){
        fileCountTotal = ds.getCount();
	byteCountTotal = ds.getSize();
    }

    public void visitDownloadProgress(final DownloadProgress dp){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    currentByteIncrement = dp.getSize();
	    progressBar.setString( "Downloading file " + (currentFileIndex+1) + " of " + fileCountTotal
				   + " : " + dp.getName()
				   + " (" + dp.getSpeed() + ")" );
	    progressBar.setValue( (int) (90f*(((float)(currentByteIndex+dp.getBytesDownloaded())) / ((float)byteCountTotal))) );
	}});
    }
    
    
    public void visitDownloadComplete(final DownloadComplete dc){
	final boolean success = dc.getSuccess();
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    currentByteIndex += currentByteIncrement;
	    currentFileIndex++;
	    if(success)
		progressBar.setString( "Download succeeded" );
	    else
		progressBar.setString( "Download failed" );
	    progressBar.setValue( 90 );
	}});
    }
    
    public void visitInstallComplete(final InstallComplete ic){
	final boolean success = ic.getSuccess();
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    if(success)
		progressBar.setString( "Installation succeeded" );
	    else
		progressBar.setString( "Installation failed.  Please try again." );
	    progressBar.setValue( 100 );
	}});
	done = true;
	successful = success;
    }

    public void visitInstallTimeout(final InstallTimeout it){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setString( "Installation timed out.  Please try again." );
	    progressBar.setValue( 100 );
	}});
	done = true;
    }
}
