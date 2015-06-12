{
    "category": "Application Control Lite",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "application_control_lite_protocol",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","application_control_lite_protocol","application_control_lite_blocked"],
    "displayOrder": 10,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "sessions",
    "title": "All Events",
    "uniqueId": "application-control-lite-EKULKEVEKE"
}
