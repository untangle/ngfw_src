{
    "uniqueId": "bandwidth-control-aAeodk0DT7",
    "category": "Bandwidth Control",
    "description": "The sum of the data sent grouped by Application Control application.",
    "displayOrder": 603,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "MB",
    "pieGroupColumn": "application_control_application",
    "pieSumColumn": "round(coalesce(sum(c2s_bytes), 0) / (1024*1024),1)",
    "readOnly": true,
    "table": "session_minutes",
    "title": "Top Application (by sent bytes)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
