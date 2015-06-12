{
    "uniqueId": "intrusion-prevention-JwTz13RT",
    "category": "Intrusion Prevention",
    "description": "The number of intrusions blocked by source IP address.",
    "displayOrder": 502,
    "enabled": true,
    "javaClass": "com.untangle.node.reporting.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "source_addr",
    "pieSumColumn": "sum(blocked::int)",
    "readOnly": true,
    "table": "intrusion_prevention_events",
    "title": "Top Source IP Addresses (blocked)",
    "type": "PIE_GRAPH"
}
