{
    "uniqueId": "virus-blocker-FGleQWZI",
    "category": "Virus Blocker",
    "description": "The top web sites by scan count.",
    "displayOrder": 107,
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
            "operator": "is",
            "value": "not null"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Web Top Scanned Sites",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
