{
    "uniqueId": "web-monitor-2joT1JbMKZw",
    "category": "Web Monitor",
    "conditions": [
        {
            "column": "web_filter_flagged",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged web request grouped by hostname.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Flagged Hostnames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
