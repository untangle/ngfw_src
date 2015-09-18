{
    "uniqueId": "bandwidth-control-V0tGdCCZ0r",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by username.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "username",
    "pieSumColumn": "round(coalesce(sum(s2p_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Usernames (by received bytes)",
    "type": "PIE_GRAPH"
}
