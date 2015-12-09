{
    "uniqueId": "bandwidth-control-GpHIa6DJmS",
    "category": "Bandwidth Control",
    "description": "The sum of the data sent grouped by Application Control category.",
    "displayOrder": 603,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "application_control_category",
    "pieSumColumn": "round(coalesce(sum(p2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Category (by sent bytes)",
    "type": "PIE_GRAPH"
}
