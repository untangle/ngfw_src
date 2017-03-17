{
    "uniqueId": "system-fgQnUn1Tle",
    "category": "System",
    "description": "The amount of free memory over time.",
    "displayOrder": 102,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "percent",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
        "round( CAST( (CAST((max(mem_total)-max(mem_free)) as float) / CAST(max(mem_total) as float)) as numeric),4)*100 as mem_usage"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "AREA",
    "title": "Memory Usage",
    "type": "TIME_GRAPH",
    "approximation": "high"
}
