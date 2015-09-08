{
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "web_filter_lite_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_lite_blocked","web_filter_lite_flagged","web_filter_lite_reason","web_filter_lite_category","s_server_addr","s_server_port"],
    "description": "Shows all blocked web requests.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "http_events",
    "title": "Blocked Web Events",
    "uniqueId": "web-filter-lite-SZC9AVRH31"
}
