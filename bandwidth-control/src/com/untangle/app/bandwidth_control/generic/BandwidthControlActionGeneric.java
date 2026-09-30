/**
 * $Id$
 */
package com.untangle.app.bandwidth_control.generic;

import com.untangle.app.bandwidth_control.BandwidthControlRuleAction;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;

/**
 * Generic (V2) action for a Bandwidth Control rule, consumed by the Vue UI.
 */
@SuppressWarnings("serial")
public class BandwidthControlActionGeneric implements JSONString, Serializable {

    public enum Type { SET_PRIORITY, TAG_HOST, GIVE_HOST_QUOTA, GIVE_USER_QUOTA }

    private Type type;
    private Integer priority;
    private String tagName;
    private Integer tagTimeSec;
    private Integer quotaTimeSec;
    private Long quotaBytes;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }

    public Integer getTagTimeSec() { return tagTimeSec; }
    public void setTagTimeSec(Integer tagTimeSec) { this.tagTimeSec = tagTimeSec; }

    public Integer getQuotaTimeSec() { return quotaTimeSec; }
    public void setQuotaTimeSec(Integer quotaTimeSec) { this.quotaTimeSec = quotaTimeSec; }

    public Long getQuotaBytes() { return quotaBytes; }
    public void setQuotaBytes(Long quotaBytes) { this.quotaBytes = quotaBytes; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    static Type fromActionType(BandwidthControlRuleAction.ActionType actionType) {
        if (actionType == null) return null;
        switch (actionType) {
            case SET_PRIORITY:   return Type.SET_PRIORITY;
            case TAG_HOST:       return Type.TAG_HOST;
            case GIVE_HOST_QUOTA: return Type.GIVE_HOST_QUOTA;
            case GIVE_USER_QUOTA: return Type.GIVE_USER_QUOTA;
            default:             return null;
        }
    }

    BandwidthControlRuleAction.ActionType toActionType() {
        if (this.type == null) return null;
        switch (this.type) {
            case SET_PRIORITY:    return BandwidthControlRuleAction.ActionType.SET_PRIORITY;
            case TAG_HOST:        return BandwidthControlRuleAction.ActionType.TAG_HOST;
            case GIVE_HOST_QUOTA: return BandwidthControlRuleAction.ActionType.GIVE_HOST_QUOTA;
            case GIVE_USER_QUOTA: return BandwidthControlRuleAction.ActionType.GIVE_USER_QUOTA;
            default:              return null;
        }
    }
}
