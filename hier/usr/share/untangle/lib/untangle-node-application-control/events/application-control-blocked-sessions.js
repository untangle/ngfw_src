{
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","application_control_application","application_control_protochain","application_control_blocked","application_control_flagged","application_control_confidence","application_control_detail"],
    "description": "All sessions matching an application control signature and blocked.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "application-control-VTK95OHTZP"
}
