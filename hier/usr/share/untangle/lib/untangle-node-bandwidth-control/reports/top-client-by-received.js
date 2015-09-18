{
    "uniqueId": "bandwidth-control-nmJBupcUo4",
    "category": "Bandwidth Control",
    "description": "The sum of the data received grouped by client address.",
    "displayOrder": 302,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "round(coalesce(sum(s2p_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Clients (by received bytes)",
    "type": "PIE_GRAPH"
}
