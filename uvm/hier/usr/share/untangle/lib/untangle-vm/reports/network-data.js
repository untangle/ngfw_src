{
    "uniqueId": "network-aGUe5wYZ1x",
    "category": "Network",
    "description": "The data usage by interface.",
    "displayOrder": 2,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "interface_id",
    "pieSumColumn": "sum(rx_bytes+tx_bytes)",
    "readOnly": true,
    "table": "interface_stat_events",
    "seriesRenderer": "interface_id",
    "title": "Data Usage (by interface)",
    "pieStyle": "COLUMN",
    "type": "PIE_GRAPH"
}
