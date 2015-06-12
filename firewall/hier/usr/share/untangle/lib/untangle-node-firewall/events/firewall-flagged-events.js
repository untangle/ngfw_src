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
            "column": "firewall_flagged",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","s_server_addr","s_server_port"],
    "displayOrder": 20,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "sessions",
    "title": "Flagged Events",
    "uniqueId": "firewall-ZO9RCJYVO2"
}
