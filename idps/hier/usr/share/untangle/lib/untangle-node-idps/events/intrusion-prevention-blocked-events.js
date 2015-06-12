{
    "category": "Intrusion Prevention",
    "conditions": [
        {
            "column": "blocked",
            "javaClass": "com.untangle.uvm.node.SqlCondition",
            "operator": "is",
            "value": "true"
        }
    ],
    "defaultColumns": ["time_stamp","sig_id","gen_id","class_id","source_addr","source_port","dest_addr","dest_port","protocol","blocked","category","classtype","msg"],
    "displayOrder": 20,
    "javaClass": "com.untangle.uvm.node.EventEntry",
    "table": "intrusion_prevention_events",
    "title": "Blocked Events",
    "uniqueId": "intrusion-prevention-EY9OO1S61X"
}
