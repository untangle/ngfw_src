/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Reviewed callable operations for SafeEventManager.
 */
public interface SafeEventManager
{
    /**
     * defaultEmailSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    Map<String, String> defaultEmailSettings();

    /**
     * emailAlertFormatPreview through the reviewed RPC contract.
     * @param rule The rule argument
     * @param event The event argument
     * @param subjectTemplate The subjectTemplate argument
     * @param bodyTemplate The bodyTemplate argument
     * @param convert The convert argument
     * @return The delegated result
     */
    Map<String, String> emailAlertFormatPreview(AlertRule rule, LogEvent event, @SafeCheckParam(SafeType.EMAIL_TEMPLATE) String subjectTemplate, @SafeCheckParam(SafeType.EMAIL_TEMPLATE) String bodyTemplate, boolean convert);

    /**
     * getClassFields through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getClassFields();

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    EventSettings getSettings();

    /**
     * getTemplateParameters through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONArray getTemplateParameters();

    /**
     * setSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    void setSettings(EventSettings newSettings);
}
