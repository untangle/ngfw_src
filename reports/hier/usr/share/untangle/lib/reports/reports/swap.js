{
    "uniqueId": "system-xejkMsUFk0",
    "category": "System",
    "description": "The swap utilization over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "bytes",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
        "(max(swap_total)-max(swap_free)) as swap_usage"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "HOUR",
    "timeStyle": "AREA",
    "title": "Swap Usage",
    "type": "TIME_GRAPH",
    "approximation": "high"
}
