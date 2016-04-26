{
    "category": "Captive Portal",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "LOGIN"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "description": "Successful logins to Captive Portal.",
    "displayOrder": 1021,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "captive_portal_user_events",
    "title": "Login Success User Events",
    "uniqueId": "captive-portal-DAVKMYJ0AR"
}
