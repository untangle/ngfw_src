{
    "uniqueId": "firewall-2joT1JbMKZw",
    "category": "Firewall",
    "description": "The number of flagged session grouped by hostname.",
    "displayOrder": 401,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "firewall_flagged",
            "operator": "=",
            "value": "true"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Hostnames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
