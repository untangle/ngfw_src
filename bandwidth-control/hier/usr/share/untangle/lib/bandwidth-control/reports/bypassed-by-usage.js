{
    "uniqueId": "bandwidth-control-48CahLowKb",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by bypassed.",
    "displayOrder": 950,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "bypassed",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes + c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Bypassed (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
