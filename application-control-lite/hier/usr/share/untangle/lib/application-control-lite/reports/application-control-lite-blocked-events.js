{
    "category": "Application Control Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "application_control_lite_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","c_client_addr","s_server_addr","s_server_port","application_control_lite_protocol","application_control_lite_blocked"],
    "description": "All sessions matching an application signature and blocked.",
    "displayOrder": 1020,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Events",
    "uniqueId": "application-control-lite-1UT1NT9YO8"
}
