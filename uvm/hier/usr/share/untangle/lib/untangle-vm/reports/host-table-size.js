{
    "uniqueId": "host-viewer-pfRvYDKKQx",
    "category": "Host Viewer",
    "description": "The amount of hosts add and removed from the host table over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "hosts",
    "readOnly": true,
    "table": "host_table_updates",
     "timeDataColumns": [
         "count(distinct(address)) as size"
    ],
    "colors": [
        "#b2b2b2"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Host Table Size",
    "type": "TIME_GRAPH"
}
