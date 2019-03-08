{
    "uniqueId": "application-control-lite-upl31dqKb1",
    "category": "Application Control Lite",
    "description": "A summary of Application Control Lite actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(application_control_lite_blocked::int) as blocked"
    ],
    "conditions": [
        {
        "javaClass": "com.untangle.app.reports.SqlCondition",
        "column": "application_control_lite_protocol",
        "operator": "is",
        "value": "not null"
       }
    ],
    "textString": "Application Control Lite detected {0} sessions and blocked {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Application Control Lite Summary",
    "type": "TEXT"
}
