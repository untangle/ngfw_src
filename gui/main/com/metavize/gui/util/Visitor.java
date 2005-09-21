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
        // XXX do something here
    }

    public void visitDownloadProgress(final DownloadProgress dp){
    SwingUtilities.invokeLater( new Runnable(){ public void run(){
        progressBar.setString( "Downloading: " + dp.getName() + " (" + dp.getSpeed() + ")");
        progressBar.setValue( (int) (50f*(((float)dp.getBytesDownloaded()) / ((float)dp.getSize()))) );
    }});
    }

    public void visitInstallComplete(final InstallComplete ic){
    final boolean success = ic.getSuccess();
    SwingUtilities.invokeLater( new Runnable(){ public void run(){
        if(success)
        progressBar.setString( "Installation succeeded" );
        else
        progressBar.setString( "Installation failed" );
        progressBar.setValue( 100 );
    }});
    done = true;
    successful = success;
    }

    public void visitDownloadComplete(final DownloadComplete dc){
    final boolean success = dc.getSuccess();
    SwingUtilities.invokeLater( new Runnable(){ public void run(){
        if(success)
        progressBar.setString( "Download succeeded" );
        else
        progressBar.setString( "Download failed" );
        progressBar.setValue( 50 );
    }});
    }

    public void visitInstallTimeout(final InstallTimeout it){
    SwingUtilities.invokeLater( new Runnable(){ public void run(){
        progressBar.setString( "Installation failed due to a time out." );
        progressBar.setValue( 100 );
    }});
    done = true;
    }
}
