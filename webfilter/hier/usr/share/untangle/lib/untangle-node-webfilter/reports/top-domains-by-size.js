{
    "uniqueId": "web-filter-lite-JKbJ2DVvAq",
    "category": "Web Filter Lite",
    "description": "The sum of the size of requested web content grouped by domain.",
    "displayOrder": 311,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "domain",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Domains (by size)",
    "type": "PIE_GRAPH"
}



