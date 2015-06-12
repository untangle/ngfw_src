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
            "value": "'INACTIVE'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 24,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "capture_user_events",
    "title": "Idle Timeout",
    "uniqueId": "captive-portal-XT3EOQP18D"
}
