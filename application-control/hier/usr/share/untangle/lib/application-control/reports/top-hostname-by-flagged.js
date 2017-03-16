{
    "uniqueId": "application-control-2joT1JbMKZw",
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_flagged",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged sessions grouped by hostname.",
    "displayOrder": 401,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Hostnames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
