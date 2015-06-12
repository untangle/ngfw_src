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
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 12,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "sessions",
    "title": "All User Events",
    "uniqueId": "captive-portal-CWKFK6AXDU"
}
