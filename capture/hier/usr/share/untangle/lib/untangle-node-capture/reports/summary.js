{
    "uniqueId": "captive-portal-upl31dqKb1",
    "category": "Captive Portal",
    "description": "A summary of Captive Portal actions.",
    "displayOrder": 11,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "textColumns": [
        "count(*) as logins"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "column": "event_info",
            "operator": "=",
            "value": "LOGIN"
        }
    ],
    "textString": "Captive Portal processed {0} user logins.", 
    "readOnly": true,
    "table": "capture_user_events",
    "title": "Captive Portal Summary",
    "type": "TEXT"
}
