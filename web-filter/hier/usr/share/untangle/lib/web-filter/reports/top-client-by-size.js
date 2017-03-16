{
    "uniqueId": "web-filter-0tHOzDh26m",
    "category": "Web Filter",
    "description": "The sum of the size of requested web content grouped by client.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Clients (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
