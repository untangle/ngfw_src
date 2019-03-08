{
    "uniqueId": "application-control-lite-UU6xjVJuQ1",
    "category": "Application Control Lite",
    "description": "The top logged sessions by host.",
    "displayOrder": 500,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_lite_blocked",
            "operator": "=",
            "value": "FALSE"
        },
        {
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
        }
    ],
    "title": "Top Logged Hosts",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

