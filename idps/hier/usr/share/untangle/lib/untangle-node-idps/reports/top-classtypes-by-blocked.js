{
    "uniqueId": "intrusion-prevention-c4BSL6qq",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by classtype.",
    "displayOrder": 302,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "classtype",
    "pieSumColumn": "count(*)",
    "conditions": [{
        "column": "blocked",
        "javaClass": "com.untangle.uvm.node.SqlCondition",
        "operator": "=",
        "value": "true"
    }],
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Classtypes (blocked)",
    "type": "PIE_GRAPH"
}
