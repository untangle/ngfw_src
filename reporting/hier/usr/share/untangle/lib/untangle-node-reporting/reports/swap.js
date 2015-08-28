{
    "uniqueId": "system-xejkMsUFk0",
    "category": "System",
    "description": "The swap utilization over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "percent",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
        "round(((max(swap_total)-max(swap_free))::float/max(swap_total)::float)::numeric,4)*100 as swap_usage"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "HOUR",
    "timeStyle": "AREA",
    "title": "Swap Usage",
    "type": "TIME_GRAPH"
}
