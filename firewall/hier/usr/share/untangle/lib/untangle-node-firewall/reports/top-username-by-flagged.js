{
    "uniqueId": "firewall-4nnbmfOVa3",
    "category": "Firewall",
    "description": "The number of flagged session grouped by username.",
    "displayOrder": 601,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
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
    "title": "Top Flagged Usernames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
