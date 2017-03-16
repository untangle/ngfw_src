{
    "uniqueId": "host-viewer-pfRvYDKKQx",
    "category": "Hosts",
    "description": "The amount of active hosts by time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hosts",
    "readOnly": true,
    "table": "server_events",
    "timeDataColumns": [
         "max(active_hosts) as active_hosts"
    ],
    "colors": [
        "#396c2b"
    ],
    "timeDataInterval": "MINUTE",
    "timeStyle": "AREA",
    "title": "Hosts Active",
    "type": "TIME_GRAPH",
    "approximation": "high"
}
