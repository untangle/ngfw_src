{
    "uniqueId": "firewall-cnxNJ0ZXsz",
    "category": "Firewall",
    "description": "The number of flagged session grouped by username.",
    "displayOrder": 602,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "username",
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
    "title": "Top Blocked Usernames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
