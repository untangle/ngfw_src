{
    "category": "Captive Portal",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "captive_portal_blocked",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","s_server_addr","s_server_port","captive_portal_rule_index","captive_portal_blocked"],
    "displayOrder": 12,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "sessions",
    "title": "Captured Session Events",
    "uniqueId": "captive-portal-MXNEEC1VG9"
}
