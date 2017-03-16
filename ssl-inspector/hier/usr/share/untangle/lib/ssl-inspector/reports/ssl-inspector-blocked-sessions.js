{
    "category": "SSL Inspector",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "ssl_inspector_status",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "BLOCKED"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","c_client_addr","s_server_addr","s_server_port","s_server_port","ssl_inspector_status","ssl_inspector_ruleid","ssl_inspector_detail"],
    "description": "Events where traffic was blocked because it did not contain a valid SSL request, and the Block Invalid Traffic option was enabled.",
    "displayOrder": 1040,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Sessions",
    "uniqueId": "ssl-inspector-VUH391EPRT"
}
