/**
 * $Id: ApplicationControlSettings.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.application_control;

import java.util.LinkedList;
import java.util.Hashtable;
import org.json.JSONString;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class ApplicationControlSettings implements java.io.Serializable, JSONString
{
    private LinkedList<ApplicationControlProtoRule> protoList = null;
    private LinkedList<ApplicationControlLogicRule> logicList = null;
    private Hashtable<String, ApplicationControlProtoRule> protoHash = null;
    private boolean daemonDebug = false;

    public LinkedList<ApplicationControlProtoRule> getProtoRules()
    {
        return (protoList);
    }

    public void setProtoRules(LinkedList<ApplicationControlProtoRule> protoList)
    {
        this.protoList = protoList;
    }

    public LinkedList<ApplicationControlLogicRule> getLogicRules()
    {
        return (logicList);
    }

    public void setLogicRules(LinkedList<ApplicationControlLogicRule> logicList)
    {
        this.logicList = logicList;
    }

    public boolean getDaemonDebug()
    {
        return (daemonDebug);
    }

    public void setDaemonDebug(boolean daemonDebug)
    {
        this.daemonDebug = daemonDebug;
    }

    public void applyNodeRules(ApplicationControlStatistics statistics)
    {
        this.protoHash = new Hashtable<String, ApplicationControlProtoRule>();
        long liveCount = 0;
        long flagCount = 0;
        long blockCount = 0;
        ApplicationControlProtoRule local;

        // add all the active protocol rules to the hashset
        for (ApplicationControlProtoRule protoRule : protoList) {
            protoHash.put(protoRule.getGuid(), protoRule);

            // block==tarpit
            // block is gone and tarpit is now block so we pick up the old flag
            // if set and then clear so it doesn't ever get picked up again 
            if (protoRule.getTarpit() == true) {
                protoRule.setBlock(true);
                protoRule.setTarpit(false);
            }

            if (protoRule.getFlag() == true) flagCount++;
            if (protoRule.getBlock() == true) blockCount++;
        }

        statistics.setProtoTotalCount(protoList.size());
        statistics.setProtoFlagCount(flagCount);
        statistics.setProtoBlockCount(blockCount);

        for (ApplicationControlLogicRule logicRule : logicList) {
            if (logicRule.isLive()) liveCount++;

            // block==tarpit
            // block is gone and tarpit is now block so we must update the logic rules
            if (logicRule.getAction().getActionType() == ApplicationControlLogicRuleAction.ActionType.TARPIT) {
                logicRule.getAction().setActionType(ApplicationControlLogicRuleAction.ActionType.BLOCK);
            }
        }

        statistics.setLogicTotalCount(logicList.size());
        statistics.setLogicLiveCount(liveCount);
    }

    public ApplicationControlProtoRule searchProtoRules(String protocol)
    {
        return (protoHash.get(protocol));
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
