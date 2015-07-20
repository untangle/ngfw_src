{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "'TIMEOUT'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "description": "Sessions that reached the session timeout.",
    "displayOrder": 23,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "capture_user_events",
    "title": "Session Timeout User Events",
    "uniqueId": "captive-portal-14EX9EL9NL"
}
