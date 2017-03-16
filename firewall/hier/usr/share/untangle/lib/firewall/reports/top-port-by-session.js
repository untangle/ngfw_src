{
    "uniqueId": "firewall-DU6NAsnJF2",
    "category": "Firewall",
    "description": "The number of scanned session grouped by server (destination) port.",
    "displayOrder": 700,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "s_server_port",
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
    "title": "Top Scanned Server Ports",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
