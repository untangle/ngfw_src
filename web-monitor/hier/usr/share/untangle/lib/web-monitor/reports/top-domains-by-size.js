{
    "uniqueId": "web-monitor-JKbJ2DVvAq",
    "category": "Web Monitor",
    "description": "The sum of the size of requested web content grouped by domain.",
    "displayOrder": 311,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "domain",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Domains (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}



