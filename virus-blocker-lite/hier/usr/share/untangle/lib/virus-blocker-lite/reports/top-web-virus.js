{
    "uniqueId": "virus-blocker-lite-IyG8eOrn",
    "category": "Virus Blocker Lite",
    "description": "The top web virus blocked.",
    "displayOrder": 104,
    "enabled": true,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "virus_blocker_lite_name",
    "pieSumColumn": "count(*)",
    "conditions": [
        {
            "column": "virus_blocker_lite_clean",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "readOnly": true,
    "table": "http_events",
    "title": "Web Top Blocked Viruses",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
