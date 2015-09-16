{
    "category": "WAN Failover",
    "conditions": [
        {
            "column": "success",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "false"
        }
    ],
    "defaultColumns": ["time_stamp","interface_id","name","description","success"],
    "description": "All tests that resulted in failure.",
    "displayOrder": 21,
    "javaClass": "com.untangle.node.reports.EventEntry",
    "table": "wan_failover_test_events",
    "title": "Failed Test Events",
    "uniqueId": "wan-failover-DXFBPITOOV"
}
