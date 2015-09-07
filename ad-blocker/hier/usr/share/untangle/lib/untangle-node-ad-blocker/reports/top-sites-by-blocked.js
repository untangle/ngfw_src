{
    "uniqueId": "ad-blocker-Bo8GBno2Bd",
    "category": "Ad Blocker",
    "description": "The number of blocked ads grouped by website.",
    "displayOrder": 304,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "host",
    "pieSumColumn": "count(*)",
     "conditions": [
        {
            "column": "ad_blocker_action",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
   "readOnly": true,
   "table": "http_events",
   "title": "Top Blocked Ad Sites",
   "type": "PIE_GRAPH"
}
