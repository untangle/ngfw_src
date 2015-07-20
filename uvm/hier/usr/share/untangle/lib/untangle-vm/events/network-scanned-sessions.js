{
    "category": "Network",
    "conditions": [
        {
            "column": "bypassed",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "false"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port"],
    "description": "All sessions that were not bypassed.",
    "displayOrder": 11,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Scanned Sessions",
    "uniqueId": "network-cCHSVFktsM"
}
