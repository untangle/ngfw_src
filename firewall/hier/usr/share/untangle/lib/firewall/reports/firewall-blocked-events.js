{
    "category": "Firewall",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "firewall_blocked",
            "javaClass": "com.untangle.app.reports.SqlCondition",
            "operator": "is",
            "value": "TRUE"
        }
    ],
    "defaultColumns": ["time_stamp","username","hostname","c_client_port","protocol","firewall_blocked","firewall_flagged","firewall_rule_index","c_client_addr","s_server_addr","s_server_port"],
    "description": "Events blocked by Firewall App.",
    "displayOrder": 1020,
    "javaClass": "com.untangle.app.reports.ReportEntry",
    "table": "sessions",
    "title": "Blocked Events",
    "uniqueId": "firewall-N195L7ZCAX"
}
