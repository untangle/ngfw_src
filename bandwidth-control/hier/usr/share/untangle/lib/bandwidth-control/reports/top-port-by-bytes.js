{
    "uniqueId": "bandwidth-control-fO1arLHiij",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by server port.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "s_server_port",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes + c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Ports (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
