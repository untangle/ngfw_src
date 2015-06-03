{
    "uniqueId": "firewall-GlzEJqEcXv",
    "category": "Firewall",
    "description": "The amount of scanned, flagged, and blocked sessions over time.",
    "displayOrder": 100,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "preCompileResults": false,
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "column": "firewall_rule_index",
            "operator": "is",
            "value": "not null"
        }
    ],
     "timeDataColumns": [
        "count(*) as scanned",
        "sum(firewall_flagged::int) as flagged",
        "sum(firewall_blocked::int) as blocked"
    ],
    "colors": [
        "#396c2b",
        "#e5e500",
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D",
    "title": "Scanned Sessions (all)",
    "type": "TIME_GRAPH"
}
