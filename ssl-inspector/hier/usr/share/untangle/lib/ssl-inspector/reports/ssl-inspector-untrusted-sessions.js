{
    "category": "SSL Inspector",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "ssl_inspector_status",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "=",
            "value": "UNTRUSTED"
        }
    ],
    "defaultColumns": ["time_stamp","hostname","username","c_client_addr","s_server_addr","s_server_port","s_server_port","ssl_inspector_status","ssl_inspector_ruleid","ssl_inspector_detail"],
    "description": "Events where traffic was blocked because the server certificate could not be authenticated.",
    "displayOrder": 1050,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Untrusted Sessions",
    "uniqueId": "ssl-inspector-0XXBNH9QVH"
}
