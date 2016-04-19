{
    "category": "Network",
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "bypassed",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","protocol","c_client_port","s_server_addr","s_server_port"],
    "description": "All sessions matching a bypass rule and bypassed.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "sessions",
    "title": "Bypassed Sessions",
    "uniqueId": "network-mKTwRemgvD"
}
