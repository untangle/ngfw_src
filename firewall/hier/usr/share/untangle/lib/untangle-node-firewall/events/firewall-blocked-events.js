{
    "category": "Firewall",
    "conditions": [
        {
            "column": "firewall_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","firewall_blocked","firewall_flagged","firewall_rule_index","s_server_addr","s_server_port"],
    "description": "Events blocked by Firewall App.",
    "displayOrder": 30,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "sessions",
    "title": "Blocked Events",
    "uniqueId": "firewall-N195L7ZCAX"
}
