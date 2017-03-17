{
    "category": "Application Control",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "application_control_application",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        },
        {
            "column": "application_control_confidence",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": ">",
            "value": "0"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","c_client_addr","s_server_addr","s_server_port","application_control_application","application_control_protochain","application_control_blocked","application_control_flagged","application_control_confidence","application_control_detail"],
    "description": "All sessions matching an application control signature.",
    "displayOrder": 1010,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Classified Sessions",
    "uniqueId": "application-control-N8MZ2K2L43"
}
