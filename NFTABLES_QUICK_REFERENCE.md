# iptables to nftables Quick Reference for NGFW

## Quick Conversion Guide

### Basic Rule Structure

```bash
# iptables (imperative - ADD to existing rules)
iptables -t mangle -A mark-src-intf -i eth0 -j MARK --set-mark 0xfa

# nftables (declarative - DEFINE rule set)
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa
```

---

## Common NGFW Patterns

### Pattern 1: Traffic Marking (QoS/Routing)

```bash
# IPTABLES
iptables -t mangle -A mark-src-intf -i eth0 -j MARK --set-mark 0xfa
iptables -t mangle -A mark-traffic -p tcp --dport 80 -j MARK --set-mark 0x1000

# NFTABLES
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa
nft add rule inet mangle mark-traffic tcp dport 80 meta mark set 0x1000
```

### Pattern 2: Dynamic Blocklists

```bash
# IPTABLES (with ipset)
ipset create dblsets hash:ip
iptables -A dynamic-block-list -m set --match-set dblsets dst -j DROP
# Dynamic add: ipset add dblsets 10.0.0.1

# NFTABLES
nft add set inet filter dblsets { type ipv4_addr; flags dynamic; }
nft add rule inet filter dynamic-block-list ip daddr @dblsets drop
# Dynamic add: nft add element inet filter dblsets { 10.0.0.1 }
```

### Pattern 3: NAT/Port Forwarding

```bash
# IPTABLES
iptables -t nat -I port-forward-rules -p tcp --dport 443 -j DNAT --to-destination 192.168.1.100:8443

# NFTABLES
nft add rule inet nat port-forward-rules tcp dport 443 dnat to 192.168.1.100:8443
```

### Pattern 4: Connection Tracking

```bash
# IPTABLES
iptables -t filter -A FORWARD -m conntrack --ctstate NEW -j dynamic-block-list

# NFTABLES
nft add rule inet filter FORWARD ct state new jump dynamic-block-list
```

### Pattern 5: Interface Masquerading (VPN)

```bash
# IPTABLES
iptables -t nat -A POSTROUTING -o tun0 -j MASQUERADE

# NFTABLES
nft add rule inet nat POSTROUTING oifname tun0 masquerade
```

### Pattern 6: Chain Jump

```bash
# IPTABLES
iptables -A INPUT -i eth0 -j my-custom-chain

# NFTABLES
nft add rule inet filter INPUT iifname eth0 jump my-custom-chain
```

---

## Table/Chain Mapping

### Filter Table (Drop/Accept traffic)
```bash
iptables -t filter -A FORWARD ...
# becomes
nft add rule inet filter FORWARD ...
```

### Mangle Table (Modify packets)
```bash
iptables -t mangle -A mark-traffic ...
# becomes
nft add rule inet mangle mark-traffic ...
```

### NAT Table (Address translation)
```bash
iptables -t nat -A POSTROUTING ...
# becomes
nft add rule inet nat POSTROUTING ...
```

---

## Match Conditions

| iptables | nftables | Example |
|----------|----------|---------|
| `-i eth0` | `iifname eth0` | Inbound interface |
| `-o eth0` | `oifname eth0` | Outbound interface |
| `-p tcp` | `tcp` | TCP protocol |
| `-p udp` | `udp` | UDP protocol |
| `--sport 443` | `tcp sport 443` | TCP source port |
| `--dport 80` | `tcp dport 80` | TCP dest port |
| `-s 10.0.0.1` | `ip saddr 10.0.0.1` | Source IP |
| `-d 10.0.0.1` | `ip daddr 10.0.0.1` | Dest IP |
| `-m conntrack --ctstate NEW` | `ct state new` | New connection |
| `-m set --match-set myset` | `@myset` | Set membership |
| `-m comment --comment "text"` | `comment "text"` | Comment |

---

## Target/Verdict Actions

| iptables | nftables | Purpose |
|----------|----------|---------|
| `-j ACCEPT` | `accept` | Allow packet |
| `-j DROP` | `drop` | Discard packet |
| `-j REJECT` | `reject` | Reject with error |
| `-j MARK --set-mark 0xfa` | `meta mark set 0xfa` | Set packet mark |
| `-j DNAT --to-destination IP:PORT` | `dnat to IP:PORT` | Destination NAT |
| `-j SNAT --to-source IP:PORT` | `snat to IP:PORT` | Source NAT |
| `-j MASQUERADE` | `masquerade` | Dynamic masquerade |
| `-j LOG --log-prefix "text"` | `log prefix "text"` | Logging |
| `-j RETURN` | `return` | Return from chain |
| `-j CHAIN_NAME` | `jump CHAIN_NAME` | Call custom chain |

---

## nftables-Specific Features (No iptables Equivalent)

### Sets (Better than ipset)
```nftables
set my_ips { type ipv4_addr; elements = { 10.0.0.1, 10.0.0.2 }; }
set dynamic_ips { type ipv4_addr; flags dynamic; }
```

### Maps
```nftables
map port_map { type tcp dport : verdict; elements = { 80 : accept, 443 : accept }; }
```

### Counters
```nftables
counter pckt_cnt { packets 100, bytes 5000 }
```

---

## NGFW-Specific Conversions

### WireGuard VPN Rules
```bash
# IPTABLES (old)
iptables -t mangle -I mark-src-intf 3 -i tun0 -j MARK --set-mark 0xfa

# NFTABLES (new)
nft add rule inet mangle mark-src-intf iifname tun0 meta mark set 0xfa
```

### WAN Balancer Routing
```bash
# IPTABLES (old)
iptables -t mangle -A wan-route-rules -p tcp --dport 80 -j MARK --set-mark 0x100

# NFTABLES (new)
nft add rule inet mangle wan-route-rules tcp dport 80 meta mark set 0x100
```

### Intrusion Prevention (Suricata)
```bash
# IPTABLES (old - nfqueue)
iptables -t mangle -A FORWARD -p tcp --dport 443 -j NFQUEUE --queue-num 2930

# NFTABLES (new)
nft add rule inet mangle FORWARD tcp dport 443 queue num 2930
```

### Dynamic Blocklists (DBL)
```bash
# IPTABLES (old)
iptables -t filter -N dynamic-block-list
iptables -t filter -A FORWARD -m conntrack --ctstate NEW -j dynamic-block-list
iptables -t filter -A dynamic-block-list -m set --match-set dblsets dst -j NFLOG --nflog-prefix "blocked"
iptables -t filter -A dynamic-block-list -m set --match-set dblsets dst -j DROP

# NFTABLES (new)
nft add chain inet filter dynamic-block-list
nft add rule inet filter FORWARD ct state new jump dynamic-block-list
set dblsets { type ipv4_addr; flags dynamic; }
nft add rule inet filter dynamic-block-list ip daddr @dblsets log prefix \"blocked\"
nft add rule inet filter dynamic-block-list ip daddr @dblsets drop
```

---

## Java Code Patterns

### Mark Application (No change in values)

```java
// BEFORE (iptables)
String cmd = "iptables -t mangle -A chain-name -i " + iface + 
             " -j MARK --set-mark 0xfa";
execManager.exec(cmd);

// AFTER (nftables)
String nftRule = "add rule inet mangle chain-name iifname " + iface + 
                 " meta mark set 0xfa";
nftablesManager.addRule(nftRule);
```

### Dynamic Blocklist Update

```java
// BEFORE (ipset)
execManager.exec("ipset add dblsets " + ipAddress);

// AFTER (nftables)
nftablesManager.addElement("inet", "filter", "dblsets", ipAddress);
// Internally: nft add element inet filter dblsets { <ipAddress> }
```

### Rule Application

```java
// BEFORE (iptables script execution)
File outputFile = new File("/etc/untangle/iptables-rules.d/XXX-app");
FileUtils.writeFile(outputFile, iptablesCommands);
execManager.exec(outputFile.getAbsolutePath());

// AFTER (nftables batch load)
nftablesManager.loadRuleSet(nftRuleSet);
// Internally: nft -f /tmp/rules.nft
```

---

## Testing Commands

### View Current Rules

```bash
# IPTABLES
iptables -t mangle -nL mark-src-intf

# NFTABLES
nft list chain inet mangle mark-src-intf
# Or view entire ruleset:
nft list ruleset
```

### Check Marks on Traffic

```bash
# Same for both - use conntrack
conntrack -L -o extended
# Look for "mark=0xfa" values
```

### Verify Set Contents

```bash
# IPTABLES (ipset)
ipset list dblsets

# NFTABLES
nft list set inet filter dblsets
```

---

## Common Mistakes to Avoid

### ❌ Wrong: Treating nftables like iptables

```bash
# WRONG - tries to add multiple rules individually
for ip in 10.0.0.1 10.0.0.2 10.0.0.3; do
    nft add rule inet filter FORWARD ip saddr $ip drop
done
# Creates 3 separate rules - inefficient, inconsistent

# RIGHT - define as set, then reference
nft add set inet filter blocked_ips { type ipv4_addr; elements = { 10.0.0.1, 10.0.0.2, 10.0.0.3 }; }
nft add rule inet filter FORWARD ip saddr @blocked_ips drop
# Single optimized rule
```

### ❌ Wrong: Forgetting to define chains

```bash
# WRONG - chain doesn't exist yet
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa

# RIGHT - define chain with hook first
nft add chain inet mangle mark-src-intf "{ type route hook input priority 0; }"
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa
```

### ❌ Wrong: Mixing table names

```bash
# WRONG - "filter" doesn't exist in iptables context
nft add table filter
nft add chain filter INPUT

# RIGHT - family must be specified
nft add table inet filter
nft add chain inet filter INPUT
```

---

## Decision Tree: iptables vs nftables in Code

```
Does code need to run on Bullseye (kernel 5.10)?
├─ YES  → Use conditional: if (isNftablesSupported()) nft_rules else iptables_rules
└─ NO   → Use nftables only

Are you writing shell scripts?
├─ YES  → Create separate scripts: script-nftables.sh + script-iptables.sh
└─ NO   → Use Java abstraction layer (NftablesRulesManager)

Are you defining many rules at once?
├─ YES  → Use nftables batch mode (load from file) for atomicity
└─ NO   → Individual rules are fine

Are you managing dynamic sets (blocklists)?
├─ YES  → Use nftables sets (more efficient than ipset)
└─ NO   → No difference, either works

Is performance critical?
├─ YES  → nftables is faster (optimized lookup)
└─ NO   → Either works, but nftables future-proof
```

---

## References

- **Full Details**: `BOOKWORM_NFTABLES_MIGRATION_PLAN.md`
- **nftables Wiki**: https://wiki.nftables.org/wiki/Quick_reference-nftables_in_10_minutes
- **Man Pages**: `man 8 nft`
- **Kernel Docs**: https://www.kernel.org/doc/html/latest/networking/nf_tables.html

---

**Quick Ref Version**: 1.0
**Last Updated**: January 29, 2026
**For**: NGFW Bullseye→Bookworm Migration
