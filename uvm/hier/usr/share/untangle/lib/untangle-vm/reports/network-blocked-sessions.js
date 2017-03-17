{
    "category": "Network",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "filter_prefix",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "not null"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","protocol","c_client_port","c_client_addr","s_server_addr","s_server_port","filter_prefix"],
    "description": "All sessions blocked by filter rules.",
    "displayOrder": 1040,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "network-ZQzCJlWkX0"
}
