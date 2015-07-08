{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "'USER_LOGOUT'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 25,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "capture_user_events",
    "title": "User Logout User Events",
    "uniqueId": "captive-portal-ZAWHSSSP3A"
}
