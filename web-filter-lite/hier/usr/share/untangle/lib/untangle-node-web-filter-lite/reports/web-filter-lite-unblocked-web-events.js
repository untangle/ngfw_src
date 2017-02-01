{
    "category": "Web Filter Lite",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "web_filter_lite_reason",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_lite_blocked","web_filter_lite_flagged","web_filter_lite_reason","web_filter_lite_category","c_client_addr","s_server_addr","s_server_port"],
    "description": "Shows all unblocked web requests",
    "displayOrder": 1040,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "http_events",
    "title": "Unblocked Web Events",
    "uniqueId": "web-filter-lite-BDQGA6HFQ0"
}
