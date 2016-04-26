{
    "category": "Intrusion Prevention",
    "readOnly": true,
    "type": "EVENT_LIST",
    "conditions": [
        {
            "column": "blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","sig_id","gen_id","class_id","source_addr","source_port","dest_addr","dest_port","protocol","blocked","category","classtype","msg"],
    "description": "All sessions matching Intrusion Prevention signatures and blocked.",
    "displayOrder": 1020,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "intrusion_prevention_events",
    "title": "Blocked Events",
    "uniqueId": "intrusion-prevention-EY9OO1S61X"
}
