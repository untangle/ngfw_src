{
    "uniqueId": "firewall-4UsnhNrCgI",
    "category": "Firewall",
    "description": "The number of flagged session grouped by server (destination) port.",
    "displayOrder": 701,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_port",
    "pieSumColumn": "sum(firewall_flagged::int)",
    "preCompileResults": false,
    "conditions": [
        {
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "column": "firewall_rule_index",
            "operator": "is",
            "value": "not null"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Server Ports",
    "type": "PIE_GRAPH"
}
