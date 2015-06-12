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
            "value": "'USER_LOGOUT'"
        }
    ],
    "defaultColumns": ["time_stamp","client_addr","login_name","event_info","auth_type"],
    "displayOrder": 25,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "capture_user_events",
    "title": "User Logout User Events",
    "uniqueId": "captive-portal-ZAWHSSSP3A"
}
