{
    "uniqueId": "virus-blocker-m6J8aPUQ",
    "category": "Virus Blocker",
    "description": "The top web sites by blocked virus count.",
    "displayOrder": 106,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Web Top Blocked Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
