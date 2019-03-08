{
    "uniqueId": "application-control-upl31dqKb1",
    "category": "Application Control",
    "description": "A summary of Application Control actions.",
    "displayOrder": 10,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "textColumns": [
        "count(*) as scanned",
        "sum(application_control_blocked::int) as blocked"
    ],
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_application",
            "operator": "is",
            "value": "not null"
        }
    ],
    "textString": "Application Control scanned {0} sessions and blocked {1} sessions.", 
    "readOnly": true,
    "table": "sessions",
    "title": "Application Control Summary",
    "type": "TEXT"
}
