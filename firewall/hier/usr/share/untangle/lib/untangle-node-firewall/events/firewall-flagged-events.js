{
    "category": "Firewall",
    "conditions": [
        {
            "column": "firewall_flagged",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","s_server_addr","s_server_port"],
    "displayOrder": 20,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "sessions",
    "title": "Flagged Events",
    "uniqueId": "firewall-ZO9RCJYVO2"
}
