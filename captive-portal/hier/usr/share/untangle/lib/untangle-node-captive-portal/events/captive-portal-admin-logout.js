{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "ADMIN_LOGOUT"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "description": "Sessions logged off by the admin.",
    "displayOrder": 26,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "capture_user_events",
    "title": "Admin Logout User Events",
    "uniqueId": "captive-portal-8B0N2VLGE6"
}
