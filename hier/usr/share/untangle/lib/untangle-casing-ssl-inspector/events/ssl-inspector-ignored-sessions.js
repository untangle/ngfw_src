{
    "category": "SSL Inspector",
    "conditions": [
        {
            "column": "ssl_inspector_status",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "IGNORED"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","s_server_addr","s_server_port","s_server_port","ssl_inspector_status","ssl_inspector_ruleid","ssl_inspector_detail"],
    "description": "Events where traffic was not or could not be inspected, so the traffic was completely ignored and not analyzed by any applications or services.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Ignored Sessions",
    "uniqueId": "ssl-inspector-4U15DL2DZM"
}
