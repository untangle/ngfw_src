{
    "category": "Web Filter Lite",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "web_filter_lite_flagged",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_lite_blocked","web_filter_lite_flagged","web_filter_lite_reason","web_filter_lite_category","s_server_addr","s_server_port"],
    "displayOrder": 20,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "http_events",
    "title": "Flagged Web Events",
    "uniqueId": "web-filter-lite-5KX7K1R864"
}
