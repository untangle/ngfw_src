{
    "category": "Application Control Lite",
    "conditions": [
        {
            "column": "application_control_lite_blocked",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","application_control_lite_protocol","application_control_lite_blocked"],
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Blocked Events",
    "uniqueId": "application-control-lite-1UT1NT9YO8"
}
