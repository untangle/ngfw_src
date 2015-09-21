{
    "category": "SSL Inspector",
    "conditions": [
        {
            "column": "ssl_inspector_status",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "ABANDONED"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","s_server_addr","s_server_port","s_server_port","ssl_inspector_status","ssl_inspector_ruleid","ssl_inspector_detail"],
    "description": "Events where traffic was blocked due to an underlying problems with the SSL session.",
    "displayOrder": 60,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Abandoned Sessions",
    "uniqueId": "ssl-inspector-W8W6WPGF0Y"
}
