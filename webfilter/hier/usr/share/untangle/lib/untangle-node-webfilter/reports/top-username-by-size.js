{
    "uniqueId": "web-filter-lite-azSIEeInZR",
    "category": "Web Filter Lite",
    "description": "The sum of the size of requested web content grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "username",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Usernames (by size)",
    "type": "PIE_GRAPH"
}
