{
    "uniqueId": "firewall-oht3kyXoUM",
    "category": "Firewall",
    "description": "The number of scanned session grouped by username.",
    "displayOrder": 600,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
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
    "title": "Top Scanned Usernames",
    "type": "PIE_GRAPH"
}
