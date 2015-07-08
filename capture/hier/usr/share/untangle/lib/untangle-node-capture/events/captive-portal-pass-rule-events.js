{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "FALSE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","captive_portal_rule_index","captive_portal_blocked"],
    "displayOrder": 11,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Passed Session Events",
    "uniqueId": "captive-portal-3O3BNGQAWH"
}
