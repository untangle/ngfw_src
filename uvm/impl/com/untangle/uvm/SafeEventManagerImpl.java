/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeEventManagerImpl implements SafeEventManager
{
    private final Supplier<EventManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeEventManagerImpl(Supplier<EventManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * defaultEmailSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Map<String, String> defaultEmailSettings()
    {
        return delegate.get().defaultEmailSettings();
    }

    /**
     * emailAlertFormatPreview through the reviewed RPC contract.
     * @param rule The rule argument
     * @param event The event argument
     * @param subjectTemplate The subjectTemplate argument
     * @param bodyTemplate The bodyTemplate argument
     * @param convert The convert argument
     * @return The delegated result
     */
    @Override
    public Map<String, String> emailAlertFormatPreview(AlertRule rule, LogEvent event, @SafeCheckParam(SafeType.EMAIL_TEMPLATE) String subjectTemplate, @SafeCheckParam(SafeType.EMAIL_TEMPLATE) String bodyTemplate, boolean convert)
    {
        return delegate.get().emailAlertFormatPreview(rule, event, subjectTemplate, bodyTemplate, convert);
    }

    /**
     * getClassFields through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getClassFields()
    {
        return delegate.get().getClassFields();
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public EventSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * getTemplateParameters through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONArray getTemplateParameters()
    {
        return delegate.get().getTemplateParameters();
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    @Override
    public void setSettings(EventSettings newSettings)
    {
        delegate.get().setSettings(newSettings);
    }
}
