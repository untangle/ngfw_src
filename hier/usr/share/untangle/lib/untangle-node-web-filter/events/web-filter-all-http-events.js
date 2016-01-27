{
    "category": "Web Filter",
    "conditions": [
        {
            "column": "web_filter_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is not",
            "value": "NULL"
        },
        {
            "column": "s_server_port",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "80"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_blocked","web_filter_flagged","web_filter_reason","web_filter_category","s_server_addr","s_server_port"],
    "description": "Shows all scanned unencrypted HTTP requests.",
    "displayOrder": 13,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "All HTTP Events",
    "uniqueId": "web-filter-V5DNHWEKT7"
}
