{
    "uniqueId": "intrusion-prevention-nBogCEbx",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by protocol.",
    "displayOrder": 902,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "protocol",
    "pieSumColumn": "count(*)",
    "conditions": [{
        "column": "blocked",
        "javaClass": "com.untangle.node.reports.SqlCondition",
        "operator": "=",
        "value": "true"
    }],
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Protocols (blocked)",
    "pieStyle": "PIE",
    "type": "PIE_GRAPH"
}
