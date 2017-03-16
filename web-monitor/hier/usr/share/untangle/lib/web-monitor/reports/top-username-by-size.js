{
    "uniqueId": "web-monitor-azSIEeInZR",
    "category": "Web Monitor",
    "description": "The sum of the size of requested web content grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "username",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Usernames (by size)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
