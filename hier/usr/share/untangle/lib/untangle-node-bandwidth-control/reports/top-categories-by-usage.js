{
    "uniqueId": "bandwidth-control-0ZRfjqOiUW",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by Application Control category.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "application_control_category",
    "pieSumColumn": "round(coalesce(sum(s2p_bytes + p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Category (by total bytes)",
    "type": "PIE_GRAPH"
}
