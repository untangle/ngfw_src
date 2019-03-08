{
    "uniqueId": "wan-failover-upl31dqKb1",
    "category": "WAN Failover",
    "description": "A summary of WAN Failover actions.",
    "displayOrder": 14,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as outages"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "action",
            "operator": "=",
            "value": "DISCONNECTED"
        }
    ],
    "textString": "WAN Failover detected {0} outages.", 
    "readOnly": true,
    "table": "wan_failover_action_events",
    "title": "WAN Failover Summary",
    "type": "TEXT"
}
