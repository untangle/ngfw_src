{
    "category": "Network",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "autoFormatValue": "false",
            "column": "c_client_addr",
            "operator": "!=",
            "value": "s_client_addr"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_addr","s_client_addr","protocol","c_client_port","s_client_port","s_server_addr","s_server_port"],
    "description": "All sessions that have been NATd by Untangle.",
    "displayOrder": 60,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "NATd Sessions",
    "uniqueId": "network-hNUcmhuNsV"
}
