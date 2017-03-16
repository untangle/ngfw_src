{
    "uniqueId": "firewall-Mpnd3b85xm",
    "category": "Firewall",
    "description": "The number of scanned session grouped by client.",
    "displayOrder": 500,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "firewall_rule_index",
            "operator": "is",
            "value": "not null"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Scanned Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
