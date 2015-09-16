{
    "category": "WAN Failover",
    "conditions": [
        {
            "column": "success",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","interface_id","name","description","success"],
    "description": "All tests that resulted in success.",
    "displayOrder": 22,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "wan_failover_test_events",
    "title": "Success Test Events",
    "uniqueId": "wan-failover-Y40VX375IH"
}
