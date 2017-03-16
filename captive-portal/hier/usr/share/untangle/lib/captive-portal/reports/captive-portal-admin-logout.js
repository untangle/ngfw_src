{
    "category": "Captive Portal",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "event_info",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "ADMIN_LOGOUT"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "description": "Sessions logged off by the admin.",
    "displayOrder": 1026,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "captive_portal_user_events",
    "title": "Admin Logout User Events",
    "uniqueId": "captive-portal-8B0N2VLGE6"
}
