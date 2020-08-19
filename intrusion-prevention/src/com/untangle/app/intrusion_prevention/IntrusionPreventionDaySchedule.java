/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;

/**
    Class to hold the manual schedule for intrusion prevention
 */
@SuppressWarnings("serial")
public class IntrusionPreventionDaySchedule implements Serializable, JSONString
{
    private Integer hour = -1;
    private Integer minute = -1;
    private String day = "None";
    private boolean enabled = true;
    private boolean isAm = true;
    private String colon = ":";

   public IntrusionPreventionDaySchedule() {}
   
   public IntrusionPreventionDaySchedule(String day) {
      this.day = day;
   }
   
   public IntrusionPreventionDaySchedule(Integer hour) {
      this.hour = hour;
      this.minute = 0;
   }

   /**
      Constructor
    */
   public IntrusionPreventionDaySchedule(Integer hour, Integer minute) {
      this.hour = hour;
      this.minute = minute;
   }

   public Integer getHour() { return this.hour; }
   public void setHour(Integer hour) { this.hour = hour; }

   public Integer getMinute() { return this.minute; }
   public void setMinute(Integer minute) { this.minute = minute; }

   public String getDay() { return this.day; }
   public void setDay(String day) { this.day = day; }

   public boolean getEnabled() { return this.enabled; }
   public void setEnabled(boolean enabled) { this.enabled = enabled; }

   public boolean getIsAm() { return this.isAm; }
   public void setIsAm(boolean isAm) { this.isAm = isAm; }

   public String getColon() { return this.colon; }
   public void setColon(String colon) { this.colon = colon; }

   public void setToDefaults(boolean isWeeklyItem) {
      this.hour = -1;
      this.minute = -1;
      this.isAm = true;
      if (isWeeklyItem)
         this.day = "None";
      else 
         this.enabled = true;
   }
   
   /**
     * Returns daySchedule as a JSON string.
     *
     * @return
     *      Server daySchedule in JSON form.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}