{
    "uniqueId": "bandwidth-control-yiCG2oFF1o",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by priority.",
    "displayOrder": 701,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "bandwidth_control_priority",
    "pieSumColumn": "round(coalesce(sum(s2p_bytes + p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Priorities (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
