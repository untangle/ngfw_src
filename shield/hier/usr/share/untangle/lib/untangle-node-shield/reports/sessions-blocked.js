{
    "uniqueId": "shield-tVO1Cx3HjO",
    "category": "Shield",
    "description": "The amount of blocked sessions over time.",
    "displayOrder": 101,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "sessions",
     "timeDataColumns": [
        "sum(shield_blocked::int) as blocked"
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D_OVERLAPPED",
    "title": "Blocked Sessions",
    "type": "TIME_GRAPH"
}
