{
    "uniqueId": "bandwidth-control-yiCG2oFF1o",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by priority.",
    "displayOrder": 801,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "bandwidth_control_priority",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes + c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Priorities (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
