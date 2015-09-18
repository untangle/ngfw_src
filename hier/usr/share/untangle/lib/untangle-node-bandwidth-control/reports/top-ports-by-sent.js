{
    "uniqueId": "bandwidth-control-Pruh0QmoOF",
    "category": "Bandwidth Control",
    "description": "The sum of the data sent grouped by server port.",
    "displayOrder": 503,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "s_server_port",
    "pieSumColumn": "round(coalesce(sum(p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Ports (by sent bytes)",
    "type": "PIE_GRAPH"
}
