{
    "uniqueId": "web-filter-3RFIFhW5XUA",
    "category": "Web Filter",
    "description": "The sum of the size of requested web content grouped by hostname.",
    "displayOrder": 401,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Hostnames (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
