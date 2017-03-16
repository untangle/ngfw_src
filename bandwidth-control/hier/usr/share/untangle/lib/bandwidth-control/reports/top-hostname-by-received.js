{
    "uniqueId": "bandwidth-control-fY73nPDWlx",
    "category": "Bandwidth Control",
    "description": "The sum of the received data grouped by hostname.",
    "displayOrder": 202,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Hostnames (by received bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
