/**
 * $Id: QoSPrioritySettings.java,v 1.00 2013/03/06 13:38:43 dmorris Exp $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * QoS settings.
 */
@SuppressWarnings("serial")
public class QosPrioritySettings implements Serializable, JSONString
{
    int priorityId;
    String priorityName;

    int uploadReservation;
    int uploadLimit;
    int downloadReservation;
    int downloadLimit;
    
    public QosPrioritySettings() {}

    public int getPriorityId() { return this.priorityId; }
    public void setPriorityId( int newValue ) { this.priorityId = newValue; }

    public String getPriorityName() { return this.priorityName; }
    public void setPriorityName( String newValue ) { this.priorityName = newValue; }

    public int getUploadReservation() { return this.uploadReservation; }
    public void setUploadReservation( int newValue ) { this.uploadReservation = newValue; }

    public int getUploadLimit() { return this.uploadLimit; }
    public void setUploadLimit( int newValue ) { this.uploadLimit = newValue; }

    public int getDownloadReservation() { return this.downloadReservation; }
    public void setDownloadReservation( int newValue ) { this.downloadReservation = newValue; }

    public int getDownloadLimit() { return this.downloadLimit; }
    public void setDownloadLimit( int newValue ) { this.downloadLimit = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}