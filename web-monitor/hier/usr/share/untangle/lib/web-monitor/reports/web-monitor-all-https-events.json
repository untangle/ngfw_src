{
    "uniqueId": "web-monitor-X743CSQQKY",
    "category": "Web Monitor",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "web_filter_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        },
        {
            "column": "s_server_port",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "443"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","host","uri","web_filter_blocked","web_filter_flagged","web_filter_reason","web_filter_category","c_client_addr","s_server_addr","s_server_port"],
    "description": "Shows all encrypted HTTPS requests.",
    "displayOrder": 1014,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "http_events",
    "title": "All HTTPS Events"
}
