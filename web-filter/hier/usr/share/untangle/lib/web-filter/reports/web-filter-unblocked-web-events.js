{
    "category": "Web Filter",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "web_filter_reason",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "B"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_blocked","web_filter_flagged","web_filter_reason","web_filter_category","c_client_addr","s_server_addr","s_server_port"],
    "description": "Shows all unblocked web requests",
    "displayOrder": 1020,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "http_events",
    "title": "Unblocked Web Events",
    "uniqueId": "web-filter-I9Y3CG2J4O"
}
