{
    "category": "Network",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "autoFormatValue": "false",
            "column": "c_client_addr",
            "operator": "!=",
            "value": "s_client_addr"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_addr","s_client_addr","c_client_port","s_client_port","s_server_addr","s_server_port"],
    "description": "All sessions that have been NATd by Untangle.",
    "displayOrder": 14,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "NATd Sessions",
    "uniqueId": "network-hNUcmhuNsV"
}
