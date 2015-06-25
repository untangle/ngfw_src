{
    "uniqueId": "captive-portal-EeDySOBc",
    "category": "Captive Portal",
    "description": "The top active users that logged in to Captive Portal.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "login_name",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "capture_user_events",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "column": "event_info",
            "operator": "=",
            "value": "LOGIN"
        }
    ],
    "title": "Top Active Users",
    "type": "PIE_GRAPH"
}

