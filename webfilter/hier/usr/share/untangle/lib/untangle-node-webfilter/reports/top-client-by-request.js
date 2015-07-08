{
    "uniqueId": "web-filter-lite-8bT09IzKJw",
    "category": "Web Filter Lite",
    "description": "The number of web requests grouped by client.",
    "displayOrder": 500,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Clients (by requests)",
    "type": "PIE_GRAPH"
}
