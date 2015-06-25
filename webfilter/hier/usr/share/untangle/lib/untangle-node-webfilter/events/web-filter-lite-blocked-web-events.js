{
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "web_filter_lite_blocked",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_lite_blocked","web_filter_lite_flagged","web_filter_lite_reason","web_filter_lite_category","s_server_addr","s_server_port"],
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "http_events",
    "title": "Blocked Web Events",
    "uniqueId": "web-filter-lite-SZC9AVRH31"
}
