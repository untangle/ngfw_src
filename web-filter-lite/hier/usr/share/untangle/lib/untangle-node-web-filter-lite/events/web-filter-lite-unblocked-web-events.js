{
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "web_filter_lite_category",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "'unblocked'"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_lite_blocked","web_filter_lite_flagged","web_filter_lite_reason","web_filter_lite_category","s_server_addr","s_server_port"],
    "description": "Shows all unblocked web requests",
    "displayOrder": 40,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "Unblocked Web Events",
    "uniqueId": "web-filter-lite-BDQGA6HFQ0"
}
