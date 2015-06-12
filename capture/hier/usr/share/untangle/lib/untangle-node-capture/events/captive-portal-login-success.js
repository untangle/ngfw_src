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
            "column": "event_info",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "=",
            "value": "'LOGIN'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 21,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "capture_user_events",
    "title": "Login Success User Events",
    "uniqueId": "captive-portal-DAVKMYJ0AR"
}
