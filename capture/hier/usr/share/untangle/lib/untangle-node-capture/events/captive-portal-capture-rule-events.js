{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","captive_portal_rule_index","captive_portal_blocked"],
    "description": "Sessions matching capture rules.",
    "displayOrder": 12,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Captured Session Events",
    "uniqueId": "captive-portal-MXNEEC1VG9"
}
