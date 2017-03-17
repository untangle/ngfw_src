{
    "uniqueId": "firewall-x4d7u259zk",
    "category": "Firewall",
    "description": "The number of blocked sessions grouped by hostname.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "column": "firewall_blocked",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Hostnames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
