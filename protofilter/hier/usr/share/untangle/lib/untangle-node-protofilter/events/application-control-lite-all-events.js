{
    "category": "Application Control Lite",
    "conditions": [
        {
            "column": "application_control_lite_protocol",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","application_control_lite_protocol","application_control_lite_blocked"],
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "All Events",
    "uniqueId": "application-control-lite-EKULKEVEKE"
}
