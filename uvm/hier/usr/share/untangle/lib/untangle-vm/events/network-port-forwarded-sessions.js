{
    "category": "Network",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "autoFormatValue": "false",
            "column": "c_server_addr",
            "operator": "!=",
            "value": "s_server_addr"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_addr","protocol","c_client_port","c_server_addr","s_server_addr","s_server_port"],
    "description": "All sessions match a port forward rule.",
    "displayOrder": 50,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Port Forwarded Sessions",
    "uniqueId": "network-XqsOpHP1cP"
}
