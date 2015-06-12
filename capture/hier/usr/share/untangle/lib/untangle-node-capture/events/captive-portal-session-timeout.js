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
            "value": "'TIMEOUT'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 23,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "capture_user_events",
    "title": "Session Timeout User Events",
    "uniqueId": "captive-portal-14EX9EL9NL"
}
