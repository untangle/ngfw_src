{
    "uniqueId": "bandwidth-control-x1qlqPepn8",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by username.",
    "displayOrder": 401,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "username",
    "pieSumColumn": "round(coalesce(sum(s2c_bytes + c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Usernames (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
