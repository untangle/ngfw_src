{
    "configured": true,
    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlSettings",
    "rules": {
        "javaClass": "java.util.LinkedList",
        "list": [
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 7
                },
                "description": "Apply Penalty Box Penalties (Server)",
                "enabled": true,
                "id": 1,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "SERVER_IN_PENALTY_BOX",
                            "value": "true"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 7
                },
                "description": "Apply Penalty Box Penalties (Client)",
                "enabled": true,
                "id": 2,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "CLIENT_IN_PENALTY_BOX",
                            "value": "true"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 1
                },
                "description": "Prioritize DNS",
                "enabled": true,
                "id": 3,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "53"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize SSH",
                "enabled": true,
                "id": 4,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "PROTOCOL",
                            "value": "TCP"
                        },
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "22"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 1
                },
                "description": "Prioritize Remote Desktop (RDP,VNC)",
                "enabled": true,
                "id": 5,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "PROTOCOL",
                            "value": "TCP"
                        },
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "3389,5300"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize eMail (POP3,POP3S,IMAP,IMAPS)",
                "enabled": true,
                "id": 6,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "110,995,143,993"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize \"Remote Access\" traffic (requires Application Control)",
                "enabled": true,
                "id": 7,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "APPLICATION_CONTROL_CATEGORY",
                            "value": "Remote Access"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 3
                },
                "description": "Deprioritize site violations (requires Web Filter)",
                "enabled": true,
                "id": 8,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "WEB_FILTER_FLAGGED",
                            "value": ""
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 4
                },
                "description": "Deprioritize Windows updates (download.windowsupdate.com)",
                "enabled": true,
                "id": 9,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*windowsupdate.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 4
                },
                "description": "Deprioritize Microsoft updates (update.microsoft.com)",
                "enabled": true,
                "id": 10,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*update.microsoft.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 4
                },
                "description": "Deprioritize dropbox.net sync",
                "enabled": true,
                "id": 11,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*dropbox.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 4
                },
                "description": "Deprioritize Bittorrent",
                "enabled": true,
                "id": 12,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "APPLICATION_CONTROL_APPLICATION",
                            "value": "BITTORRE"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize pandora streaming audio",
                "enabled": true,
                "id": 13,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*pandora.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 1
                },
                "description": "Prioritize last.fm streaming audio",
                "enabled": true,
                "id": 14,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*last.fm"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize HTTP to Games sites",
                "enabled": true,
                "id": 15,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "WEB_FILTER_CATEGORY",
                            "value": "Games"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 1
                },
                "description": "Prioritize Hulu streaming video",
                "enabled": true,
                "id": 16,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*hulu.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 1
                },
                "description": "Prioritize Netflix streaming video",
                "enabled": true,
                "id": 17,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "netflix.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 6
                },
                "description": "Limit dropbox.com sync",
                "enabled": true,
                "id": 18,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_HOST",
                            "value": "*dropbox.com"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 3
                },
                "description": "Do not Prioritize large HTTP downloads (>10meg)",
                "enabled": true,
                "id": 19,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "invert": false,
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "HTTP_CONTENT_LENGTH",
                            "value": ">10000000"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize HTTP",
                "enabled": true,
                "id": 20,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "80"
                        }
                    ]
                },
                "ruleId": 12345
            },
            {
                "action": {
                    "actionType": "SET_PRIORITY",
                    "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                    "priority": 2
                },
                "description": "Prioritize HTTPS",
                "enabled": true,
                "id": 21,
                "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRule",
                "conditions": {
                    "javaClass": "java.util.LinkedList",
                    "list": [
                        {
                            "javaClass": "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                            "conditionType": "DST_PORT",
                            "value": "443"
                        }
                    ]
                },
                "ruleId": 12345
            }
        ]
    },
    "settingsVersion": 5
}
