{
    "uniqueId": "application-control-lite-Zs6Nhr1tJ9",
    "category": "Application Control Lite",
    "description": "The top logged sessions by protocol.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "application_control_lite_protocol",
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
    "title": "Top Logged Protocols",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}

