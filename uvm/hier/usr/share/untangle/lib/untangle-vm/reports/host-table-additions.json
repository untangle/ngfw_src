{
    "uniqueId": "host-viewer-UkYvElV11f",
    "category": "Hosts",
    "description": "The amount of hosts add and removed from the host table over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderDesc": false,
    "units": "hosts",
    "readOnly": true,
    "table": "host_table_updates",
     "timeDataColumns": [
        "sum(CASE WHEN key='add' THEN 1 ELSE 0 END) as add",
        "sum(CASE WHEN key='remove' THEN 1 ELSE 0 END) as removed"
    ],
    "colors": [
        "#396c2b",
        "#3399ff"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR",
    "title": "Hosts Additions",
    "type": "TIME_GRAPH"
}
