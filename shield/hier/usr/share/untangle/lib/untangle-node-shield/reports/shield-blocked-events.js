{
    "category": "Shield",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "shield_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","shield_blocked"],
    "description": "All sessions blocked by Shield.",
    "displayOrder": 1011,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Session Events",
    "uniqueId": "shield-1N0Xpss80O"
}
