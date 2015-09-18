{
    "settingsVersion":5,
    "configured":true,
    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlSettings",
    "rules":{
        "javaClass":"java.util.LinkedList",
        "list":[{
            "set" : "standard",
            "ruleId":12345,
            "description":"Apply Penalty Box Penalties (Server)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"true",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"SERVER_IN_PENALTY_BOX"
                }]
            },
            "action": {
                "actionType": "SET_PRIORITY", 
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction", 
                "priority": 7
            }, 
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Apply Penalty Box Penalties (Client)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"true",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"CLIENT_IN_PENALTY_BOX"
                }]
            },
            "action": {
                "actionType": "SET_PRIORITY", 
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction", 
                "priority": 7
            }, 
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize DNS",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"53",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":1,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize SSH",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"TCP",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"PROTOCOL"
                }, {
                    "value":"22",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize Remote Desktop (RDP,VNC)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"TCP",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"PROTOCOL"
                }, {
                    "value":"3389,5300",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":1,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize eMail (POP3,POP3S,IMAP,IMAPS)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"110,995,143,993",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize \"Remote Access\" traffic (requires Application Control)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"Remote Access",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"APPLICATION_CONTROL_CATEGORY"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description" : "Deprioritize \"Unproductive\" Applications (requires Application Control)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "invert":false,
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "value":"<2",
                    "matcherType":"APPLICATION_CONTROL_PRODUCTIVITY"
                }]
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "action":{
                "quotaBytes":null,
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "quotaTime":null,
                "actionType":"SET_PRIORITY",
                "penaltyTime":null
            },
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Deprioritize site violations (requires Web Filter)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"WEB_FILTER_FLAGGED"
                }]
            },
            "action":{
                "priority":3,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,home",
            "ruleId":12345,
            "description":"Deprioritize Windows updates (download.windowsupdate.com)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*windowsupdate.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,home",
            "ruleId":12345,
            "description":"Deprioritize Microsoft updates (update.microsoft.com)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*update.microsoft.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "metered",
            "ruleId":12345,
            "description":"Limit Microsoft updates (windowsupdates.com)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*windowsupdates.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":6,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "metered",
            "ruleId":12345,
            "description":"Limit Microsoft updates (update.microsoft.com)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*update.microsoft.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":6,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Deprioritize dropbox.net sync",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*dropbox.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description":"Penalty Box Bittorrent users for 30 minutes (requires Application Control Lite)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"Bittorrent",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"PROTOCOL_CONTROL_SIGNATURE"
                }]
            },
            "action":{
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"PENALTY_BOX_CLIENT_HOST",
                "penaltyTime":1800
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description":"Penalty Box Bittorrent users for 30 minutes (requires Application Control)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"BITTORRE",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"APPLICATION_CONTROL_APPLICATION"
                }]
            },
            "action":{
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"PENALTY_BOX_CLIENT_HOST",
                "penaltyTime":1800
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description":"Deprioritize P2P traffic (requires Application Control Lite)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"Peer to Peer",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"PROTOCOL_CONTROL_CATEGORY"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description":"Deprioritize File Transfers (requires Application Control)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"File Transfer",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"APPLICATION_CONTROL_CATEGORY"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "business,school,metered",
            "ruleId":12345,
            "description":"Deprioritize HTTP to Download Sites (requires Web Filter)",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"Download Sites",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"WEB_FILTER_CATEGORY"
                }]
            },
            "action":{
                "priority":4,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "home",
            "ruleId":12345,
            "description":"Prioritize pandora streaming audio",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*pandora.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "home",
            "ruleId":12345,
            "description":"Prioritize last.fm streaming audio",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*last.fm",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":1,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "home",
            "ruleId":12345,
            "description":"Prioritize HTTP to Games sites",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"Games",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"WEB_FILTER_CATEGORY"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "home",
            "ruleId":12345,
            "description":"Prioritize Hulu streaming video",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*hulu.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":1,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "home",
            "ruleId":12345,
            "description":"Prioritize Netflix streaming video",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"netflix.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":1,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Limit dropbox.com sync",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"*dropbox.com",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"HTTP_HOST"
                }]
            },
            "action":{
                "priority":6,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        },{
            "set" : "standard",
            "ruleId": 12345, 
            "description": "Do not Prioritize large HTTP downloads (>10meg)", 
            "matchers": {
                "javaClass": "java.util.LinkedList", 
                "list": [
                    {
                        "invert": false, 
                        "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher", 
                        "matcherType": "HTTP_CONTENT_LENGTH", 
                        "value": ">10000000"
                    }
                ]
            },
            "action": {
                "actionType": "SET_PRIORITY", 
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction", 
                "priority": 3
            }, 
            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule", 
            "enabled": true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize HTTP",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"80",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }, {
            "set" : "standard",
            "ruleId":12345,
            "description":"Prioritize HTTPS",
            "matchers":{
                "javaClass":"java.util.LinkedList",
                "list":[{
                    "value":"443",
                    "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleMatcher",
                    "matcherType":"DST_PORT"
                }]
            },
            "action":{
                "priority":2,
                "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                "actionType":"SET_PRIORITY"
            },
            "javaClass":"com.untangle.node.bandwidth_control.BandwidthControlRule",
            "enabled":true
        }]
    }
}