{
    "uniqueId": "application-control-n4Pg0YqGRn",
    "category": "Application Control",
    "description": "The number of flagged sessions grouped by application.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "application_control_flagged",
            "operator": "=",
            "value": "true"
        }
    ],
    "title": "Top Flagged Applications",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
