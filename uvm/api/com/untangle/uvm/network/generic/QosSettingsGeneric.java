/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.network.QosPriority;
import com.untangle.uvm.network.QosRule;
import com.untangle.uvm.network.QosSettings;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * QoS Settings Generic
 */
@SuppressWarnings("serial")
public class QosSettingsGeneric implements Serializable, JSONString {

    private String queueDiscipline = "fq_codel";
    private boolean qosEnabled = false;

    private int defaultPriority = 3;

    private int pingPriority = 1;
    private int dnsPriority = 1;
    private int sshPriority = 1;
    private int openvpnPriority = 1;

    private LinkedList<RuleGeneric> qos_rules = null;
    private LinkedList<QosPriority> qosPriorities = new LinkedList<>();

    public String getQueueDiscipline() { return queueDiscipline; }
    public void setQueueDiscipline(String queueDiscipline) { this.queueDiscipline = queueDiscipline; }
    public boolean isQosEnabled() { return qosEnabled; }
    public void setQosEnabled(boolean qosEnabled) { this.qosEnabled = qosEnabled; }

    public int getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(int defaultPriority) { this.defaultPriority = defaultPriority; }

    public int getPingPriority() { return pingPriority; }
    public void setPingPriority(int pingPriority) { this.pingPriority = pingPriority; }
    public int getDnsPriority() { return dnsPriority; }
    public void setDnsPriority(int dnsPriority) { this.dnsPriority = dnsPriority; }
    public int getSshPriority() { return sshPriority; }
    public void setSshPriority(int sshPriority) { this.sshPriority = sshPriority; }
    public int getOpenvpnPriority() { return openvpnPriority; }
    public void setOpenvpnPriority(int openvpnPriority) { this.openvpnPriority = openvpnPriority; }

    public LinkedList<QosPriority> getQosPriorities() { return qosPriorities; }
    public void setQosPriorities(LinkedList<QosPriority> qosPriorities) { this.qosPriorities = qosPriorities; }
    public LinkedList<RuleGeneric> getQos_rules() { return qos_rules; }
    public void setQos_rules(LinkedList<RuleGeneric> qos_rules) { this.qos_rules = qos_rules; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link QosSettingsGeneric} object into its v1 QosSettings representation.
     * @param qosSettings QosSettings
     * @return QosSettings
     */
    public QosSettings transformGenericToQosSettings(QosSettings qosSettings) {
        qosSettings.setQueueDiscipline(this.getQueueDiscipline());
        qosSettings.setQosEnabled(this.isQosEnabled());

        qosSettings.setDefaultPriority(this.getDefaultPriority());

        qosSettings.setPingPriority(this.getPingPriority());
        qosSettings.setDnsPriority(this.getDnsPriority());
        qosSettings.setSshPriority(this.getSshPriority());
        qosSettings.setOpenvpnPriority(this.getOpenvpnPriority());

        // Set QoS Priorities
        if (this.getQosPriorities() != null)
            qosSettings.setQosPriorities(this.getQosPriorities());

        // Set QoS Rules
        if (this.getQos_rules() != null)
            qosSettings.setQosRules(RuleGeneric.transformGenericToQoSRules(this.getQos_rules(), qosSettings.getQosRules()));

        return qosSettings;
    }
}
