{
    "category": "Network",
    "conditions": [
        {
            "column": "bypassed",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port"],
    "displayOrder": 12,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "All Bypassed Sessions",
    "uniqueId": "network-mKTwRemgvD"
}
