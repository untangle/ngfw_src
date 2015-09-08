{
    "category": "Network",
    "conditions": [
        {
            "column": "bypassed",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port"],
    "description": "All sessions matching a bypass rule and bypassed.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Bypassed Sessions",
    "uniqueId": "network-mKTwRemgvD"
}
