{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "event_info",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": "'ADMIN_LOGOUT'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 26,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "capture_user_events",
    "title": "Admin Logout User Events",
    "uniqueId": "captive-portal-8B0N2VLGE6"
}
