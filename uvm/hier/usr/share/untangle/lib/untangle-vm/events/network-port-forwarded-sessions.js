{
    "category": "Network",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "autoFormatValue": "false",
            "column": "c_server_addr",
            "operator": "!=",
            "value": "s_server_addr"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_addr","c_client_port","c_server_addr","s_server_addr","s_server_port"],
    "displayOrder": 13,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "All Port Forwarded Sessions",
    "uniqueId": "network-XqsOpHP1cP"
}
