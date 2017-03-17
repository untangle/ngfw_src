{
    "uniqueId": "application-control-y9CkEGQbew",
    "category": "Application Control",
    "description": "The number of sessions grouped by application.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "application_control_application",
            "operator": "is",
            "value": "not null"
        }
    ],
    "title": "Top Applications (by sessions)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
