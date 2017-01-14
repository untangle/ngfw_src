{
    "uniqueId": "system-N63OfrLqbS",
    "category": "System",
    "description": "The swap utilization over time as a percent of total memory size .",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "percent",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
        "round(((max(swap_total)-max(swap_free))::float/(max(mem_total)+1)::float)::numeric,4)*100 as swap_usage"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "HOUR",
    "timeStyle": "AREA",
    "title": "Swap Usage Ratio",
    "type": "TIME_GRAPH"
}
