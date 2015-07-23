{
    "category": "Network",
    "conditions": [
        {
            "column": "filter_prefix",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "not null"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port"],
    "description": "All sessions blocked by filter rules.",
    "displayOrder": 40,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "network-ZQzCJlWkX0"
}
