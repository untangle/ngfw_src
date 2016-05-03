{
    "uniqueId": "bandwidth-control-f380icPxbd",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by username.",
    "displayOrder": 403,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "username",
    "pieSumColumn": "round(coalesce(sum(p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Usernames (by sent bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
