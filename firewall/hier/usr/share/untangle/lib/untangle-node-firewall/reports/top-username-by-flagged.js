{
    "uniqueId": "firewall-4nnbmfOVa3",
    "category": "Firewall",
    "description": "The number of flagged session grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
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
    "title": "Top Flagged Usernames",
    "type": "PIE_GRAPH"
}
