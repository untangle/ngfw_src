{
    "uniqueId": "web-monitor-YUeFXRLG2s",
    "category": "Web Monitor",
    "description": "The number of web requests grouped by domain.",
    "displayOrder": 310,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "domain",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Domains (by request)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
