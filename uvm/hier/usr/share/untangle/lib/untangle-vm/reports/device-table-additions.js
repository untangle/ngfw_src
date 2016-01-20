{
    "uniqueId": "device-table-UkYvElV11f",
    "category": "Device List",
    "description": "The amount of devices add and removed from the device table over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "devices",
    "readOnly": true,
    "table": "device_table_updates",
     "timeDataColumns": [
        "sum(CASE WHEN key='add' THEN 1 ELSE 0 END) as add"
    ],
    "colors": [
        "#396c2b",
        "#3399ff"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D",
    "title": "Device Table Additions",
    "type": "TIME_GRAPH"
}
