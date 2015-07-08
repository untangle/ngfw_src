{
    "uniqueId": "web-filter-lite-0tHOzDh26m",
    "category": "Web Filter Lite",
    "description": "The sum of the size of requested web content grouped by client.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Clients (by size)",
    "type": "PIE_GRAPH"
}
