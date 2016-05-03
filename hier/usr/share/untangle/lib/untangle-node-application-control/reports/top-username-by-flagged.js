{
    "uniqueId": "application-control-tZ6ULGGwUy",
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_flagged",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged sessions grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Usernames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
