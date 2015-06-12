{
    "category": "Firewall",
    "conditions": [
        {
            "column": "policy_id",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": ":policyId"
        },
        {
            "column": "firewall_rule_index",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "NOT NULL"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","s_server_addr","s_server_port"],
    "displayOrder": 10,
    "javaClass": "com.untangle.uvm.node.EventLogEntry",
    "table": "sessions",
    "title": "All Events",
    "uniqueId": "firewall-JWVPEDU3Y6"
}
