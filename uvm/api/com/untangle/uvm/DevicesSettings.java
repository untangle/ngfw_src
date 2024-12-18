/**
 * $Id$
 */
package com.untangle.uvm;

 import org.json.JSONObject;
 import org.json.JSONString;

 import java.io.Serializable;
 import java.util.LinkedList;

 /**
  * Modal class for Devices Settings
  */
 @SuppressWarnings("serial")
 public class DevicesSettings implements Serializable, JSONString {

     private int version = 1;
     private LinkedList<DeviceTableEntry> devices = new LinkedList<>();
     private boolean autoDeviceRemove = false;
     private int autoRemovalThreshold = 30;         // In days

     /**
      * Get the version of settings
      * @return version
      */
     public int getVersion() { return version; }
     /**
      * Set the version of settings
      * @param version of settings
      */
     public void setVersion(int version) { this.version = version; }

     /**
      * Get the list of devices
      * @return List of {@link com.untangle.uvm.DeviceTableEntry}
      */
     public LinkedList<DeviceTableEntry> getDevices() { return devices; }
     /**
      * Set the list of devices
      * @param devices of {@link com.untangle.uvm.DeviceTableEntry}
      */
     public void setDevices(LinkedList<DeviceTableEntry> devices) { this.devices = devices; }

     /**
      * returns autoDeviceRemove
      * @return boolean for auto device removal
      */
     public boolean isAutoDeviceRemove() { return autoDeviceRemove; }
     /**
      * Set autoDeviceRemove
      * @param autoDeviceRemove boolean for auto device removal
      */
     public void setAutoDeviceRemove(boolean autoDeviceRemove) { this.autoDeviceRemove = autoDeviceRemove; }

     /**
      * Get the threshold in days after which inactive devices will be removed
      * @return days of inactivity after which a device should be removed.
      */
     public int getAutoRemovalThreshold() { return autoRemovalThreshold; }
     /**
      * Set the threshold in days after which inactive devices will be removed
      * @param autoRemovalThreshold days of inactivity after which a device should be removed.
      */
     public void setAutoRemovalThreshold(int autoRemovalThreshold) { this.autoRemovalThreshold = autoRemovalThreshold; }

     /**
      * Returns string representation of this object
      * @return string representation of this object
      */
     @Override
     public String toJSONString() {
         JSONObject jO = new JSONObject(this);
         return jO.toString();
     }
 }
