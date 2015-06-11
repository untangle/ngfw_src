{
    "uniqueId": "firewall-itTkk5YKUC",
    "category": "Firewall",
    "description": "The number of scanned session grouped by hostname.",
    "displayOrder": 400,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
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
    "title": "Top Scanned Hostnames",
    "type": "PIE_GRAPH"
}
