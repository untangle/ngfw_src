{
    "category": "Captive Portal",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","c_client_addr","s_server_addr","s_server_port","captive_portal_rule_index","captive_portal_blocked"],
    "description": "Sessions matching capture rules.",
    "displayOrder": 1012,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Captured Session Events",
    "uniqueId": "captive-portal-MXNEEC1VG9"
}
