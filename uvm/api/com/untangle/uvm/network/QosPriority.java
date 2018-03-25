/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * QoS settings.
 */
@SuppressWarnings("serial")
public class QosPriority implements Serializable, JSONString
{
    int priorityId;
    String priorityName;

    float uploadReservation;
    float uploadLimit;
    float downloadReservation;
    float downloadLimit;
    
    public QosPriority( int priorityId, String priorityName, float uploadReservation, float uploadLimit, float downloadReservation, float downloadLimit)
    {
        setPriorityId( priorityId );
        setPriorityName( priorityName );
        setUploadReservation( uploadReservation );
        setUploadLimit( uploadLimit );
        setDownloadReservation( downloadReservation );
        setDownloadLimit( downloadLimit );
    }

    public QosPriority() {}

    public int getPriorityId() { return this.priorityId; }
    public void setPriorityId( int newValue ) { this.priorityId = newValue; }

    public String getPriorityName() { return this.priorityName; }
    public void setPriorityName( String newValue ) { this.priorityName = newValue; }

    public float getUploadReservation() { return this.uploadReservation; }
    public void setUploadReservation( float newValue ) { this.uploadReservation = newValue; }

    public float getUploadLimit() { return this.uploadLimit; }
    public void setUploadLimit( float newValue ) { this.uploadLimit = newValue; }

    public float getDownloadReservation() { return this.downloadReservation; }
    public void setDownloadReservation( float newValue ) { this.downloadReservation = newValue; }

    public float getDownloadLimit() { return this.downloadLimit; }
    public void setDownloadLimit( float newValue ) { this.downloadLimit = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}