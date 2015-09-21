{
    "category": "SSL Inspector",
    "conditions": [
        {
            "column": "ssl_inspector_status",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "INSPECTED"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","s_server_addr","s_server_port","s_server_port","ssl_inspector_status","ssl_inspector_ruleid","ssl_inspector_detail"],
    "description": "Events where traffic was fully processed by the inspector, and all traffic was passed through all the other applications and services.",
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Inspected Sessions",
    "uniqueId": "ssl-inspector-ITSNWIVXOF"
}
