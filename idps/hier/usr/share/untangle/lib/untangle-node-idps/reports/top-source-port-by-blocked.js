{
    "uniqueId": "intrusion-prevention-dTCBcCU6",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by source port.",
    "displayOrder": 602,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "source_port",
    "pieSumColumn": "count(*)",
    "conditions": [{
        "column": "blocked",
        "javaClass": "com.untangle.node.reporting.SqlCondition",
        "operator": "=",
        "value": "true"
    }],
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Source Port (blocked)",
    "type": "PIE_GRAPH"
}
