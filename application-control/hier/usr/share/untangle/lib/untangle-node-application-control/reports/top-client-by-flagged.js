{
    "uniqueId": "application-control-iVSJMxo2XO",
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_flagged",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of flagged sessions grouped by client.",
    "displayOrder": 501,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "c_client_addr",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Flagged Clients",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
