{
    "uniqueId": "bandwidth-control-CRntw4hkHn",
    "category": "Bandwidth Control",
    "description": "The sum of the data transferred grouped by hostname.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "round(coalesce(sum(s2p_bytes + p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Hostnames (by total bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
