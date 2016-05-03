{
    "uniqueId": "bandwidth-control-wf0AHZ0yHR",
    "category": "Bandwidth Control",
    "description": "The sum of the data sent grouped by client address.",
    "displayOrder": 303,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "round(coalesce(sum(p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Clients (by sent bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
