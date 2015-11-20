{
    "category": "Network",
    "conditions": [
        {
            "column": "filter_prefix",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "not null"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","protocol","c_client_port","s_server_addr","s_server_port","filter_prefix"],
    "description": "All sessions blocked by filter rules.",
    "displayOrder": 40,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "network-ZQzCJlWkX0"
}
