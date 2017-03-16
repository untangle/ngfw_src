{
    "category": "Application Control",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "application_control_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","c_client_addr","s_server_addr","s_server_port","application_control_application","application_control_protochain","application_control_blocked","application_control_flagged","application_control_confidence","application_control_detail"],
    "description": "All sessions matching an application control signature and blocked.",
    "displayOrder": 1030,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "application-control-VTK95OHTZP"
}
