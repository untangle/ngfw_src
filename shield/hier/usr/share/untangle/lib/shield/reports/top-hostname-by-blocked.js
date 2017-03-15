{
    "uniqueId": "shield-81IWC45RM4",
    "category": "Shield",
    "description": "The number of blocked sessions grouped by hostname.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "filter_prefix",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "shield_blocked"
        }
    ],
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Hostnames",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
