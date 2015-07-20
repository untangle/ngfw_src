{
    "category": "Administration",
    "conditions": [],
    "defaultColumns": ["time_stamp","login","local","client_addr","succeeded","reason"],
    "description": "All local administrator logins.",
    "displayOrder": 10,
    "javaClass": "com.untangle.node.reporting.EventEntry",
    "table": "admin_logins",
    "conditions": [
        {
            "column": "login",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "!=",
            "value": "localadmin"
        },
        {
            "column": "client_addr",
            "javaClass": "com.untangle.node.reporting.SqlCondition",
            "operator": "!=",
            "value": "127.0.0.1"
        }
    ],
    "title": "Admin Logins",
    "uniqueId": "administration-9cVz18dM"
}
