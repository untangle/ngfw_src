{
    "uniqueId": "system-6iYMGsnldQ",
    "category": "System",
    "description": "The disk utilization over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "percent",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
        "round(((max(disk_total)-max(disk_free))::float/max(disk_total)::float)::numeric,4)*100 as disk_usage"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "HOUR",
    "timeStyle": "LINE",
    "title": "Disk Usage",
    "type": "TIME_GRAPH"
}
