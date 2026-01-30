# Bullseye to Bookworm Migration: iptables to nftables

## Executive Summary

**Current State:** ngfw_src and sync-settings are entirely based on iptables (Linux kernel 5.10)
**Target State:** Migrate to nftables (Linux kernel 6.x in Bookworm) while maintaining feature parity

**Scope Impact:**
- **152 iptables references** across 21 Java files
- **132 iptables references** across 11 shell scripts
- **8 custom kernel patches** that extend netfilter functionality
- **Multiple applications** that directly manipulate firewall rules

---

## Part 1: Architecture & Current iptables Usage

### 1.1 Core Network Processing Pipeline

```
Kernel Netfilter (nfqueue/conntrack)
    ↓
libnetcap (C library - nfqueue/conntrack interaction)
    ↓
jnetcap (Java wrapper - iptables rules injection)
    ↓
jvector (Traffic distribution to apps)
    ↓
Applications (firewall, threat-prevention, web-filter, etc.)
```

### 1.2 iptables Usage Categories

#### A. **Rule Management Scripts** (11 shell scripts)
Primary location: `/etc/untangle/iptables-rules.d/`

Scripts generate and apply iptables rules:
- **ut-uvm-update-rules.sh** - Main rule orchestrator (nfqueue, marks, redirect)
- **dbl-setup.sh / dbl-cleanup.sh** - Dynamic blocklist (ipset, mangle, filter chains)
- **tunnel-vpn-up.sh / tunnel-vpn-down.sh** - VPN rules (mangle table)
- **Individual app scripts** - Generated at runtime by Java apps

**Key Pattern:** Apps write shell scripts to `/etc/untangle/iptables-rules.d/XXX-<app>` and execute them

#### B. **Java Configuration** (21 files, 152 references)
Rule generation & management:

| Component | Purpose | Files |
|-----------|---------|-------|
| **Core Infrastructure** | Rule execution, mark handling | HostTableImpl, NetFilterLogger, NetworkManagerImpl |
| **Session Management** | TCP/UDP session marks (QoS) | SessionTableImpl, SessionMonitorImpl |
| **Applications** | App-specific rules | WireGuardVpnManager, OpenVpnManager, WanBalancerApp, FtpNatHandler |
| **Settings** | iptables queue numbers, nfqueue refs | IntrusionPreventionSettings |

**Key Patterns:**
- Writing scripts with embedded iptables commands
- Calling `execManager().exec()` to run scripts
- Reading from `/proc/net/netfilter/nfnetlink_queue` for queue status
- Using marks (0x01000000, 0x80000000) for traffic classification

#### C. **Kernel Patches for Untangle-Specific Features**

From `/patches/untangle/`:
1. **0002-extensions.patch** - NFMARK in cmsg (UDP packet handling)
2. **0004-iptables-tune.patch** - Dedicated "tune" table (custom processing)
3. **0005-xt_mac.patch** - MAC byte matching (interface mark restoration)
4. **0006-iptables-socket.patch** - Socket mark restoration (QoS/ingress)

**Critical:** These patches MUST be migrated to nftables infrastructure

---

## Part 2: nftables Migration Strategy

### 2.1 Why nftables (and why it's different)

| Aspect | iptables | nftables |
|--------|----------|----------|
| **API** | Multiple tools (iptables, ip6tables, arptables) | Single unified `nft` command |
| **Rule Format** | Imperative (add/delete/modify) | Declarative (set/define) |
| **Tables/Chains** | Implicit, created on-the-fly | Explicitly defined |
| **Performance** | Traverses all rules sequentially | Rule sets with optimized lookup |
| **State** | In-memory (lost on reboot w/o save) | Can be defined in files |
| **Atomicity** | Per-rule | Batch operations possible |

### 2.2 Key nftables Concepts

**Families:** `inet`, `ip`, `ip6`, `arp`, `bridge`, `netdev` (vs. iptables tables)

**Tables & Chains:**
```nftables
table inet mangle {
    chain INPUT { ... }
    chain FORWARD { ... }
}
```

**Maps & Sets:** (replaces ipset)
```nftables
set dblsets {
    type ipv4_addr
    flags dynamic
    elements = { 10.0.0.1, 10.0.0.2 }
}
```

**Rules with Statements:**
```nftables
chain mark-traffic {
    ip protocol tcp dport 80 meta mark set 0xfa
    ip saddr @srcset ct mark set 0x1000
}
```

---

## Part 3: Detailed Migration Plan (Phased Approach)

### Phase 1: Foundation & Tooling (Weeks 1-2)

**Goal:** Establish nftables infrastructure without touching jnetcap/libnetcap

#### 1.1 Python Abstraction Layer
**Location:** `sync-settings/sync/nftables_rules_manager.py` (NEW)

```python
class NftablesRulesManager:
    """Abstraction to handle rule generation for both iptables (bullseye) and nftables (bookworm)"""
    
    def __init__(self, use_nftables=True, kernel_version=None):
        self.use_nftables = use_nftables
        self.kernel_version = kernel_version or self._detect_kernel()
    
    def generate_mark_rules(self, interface, src_mark, dst_mark):
        """Generate rules for marking traffic"""
        if self.use_nftables:
            return self._nft_mark_rules(...)
        else:
            return self._iptables_mark_rules(...)
    
    def generate_filter_rules(self, direction, protocol, port, action):
        """Generate filter rules"""
        # Handles both rule formats
        
    def execute_rules(self, rules):
        """Execute rules atomically"""
        if self.use_nftables:
            return self._nft_batch_load(rules)
        else:
            return self._iptables_apply(rules)
```

#### 1.2 Documentation & Kernel Patches
- **Analyze patch requirements** for nftables equivalents
  - `0002-extensions.patch` → nftables meta mark expressions
  - `0004-iptables-tune.patch` → custom hook chain
  - `0005-xt_mac.patch` → nftables packet manipulation
  - `0006-iptables-socket.patch` → nftables socket lookup extensions
- Create nftables kernel patch compatibility document
- Define nftables rule structure for Untangle

#### 1.3 Testing Infrastructure
- Create test suite for rule generation (both formats)
- Add nftables syntax validators
- Build mock environment tests

---

### Phase 2: Shell Script Migration (Weeks 3-4)

**Goal:** Replace iptables shell scripts with nftables equivalents

#### 2.1 Rewrite Core Rule Scripts

**Priority 1: ut-uvm-update-rules.sh** (Currently ~230 lines of iptables)

Migrate from:
```bash
IPTABLES="/sbin/iptables -w"
${IPTABLES} -t mangle -A mark-src-intf -i eth0 -j MARK --set-mark 0xfa
${IPTABLES} -t filter -A FORWARD -m conntrack --ctstate NEW -j dynamic-block-list
```

To nftables structure:
```bash
nft add table inet mangle
nft add chain inet mangle mark-src-intf "{ type route hook input priority 0; }"
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa
```

**Key Migration Points:**
- Mangle table → `inet mangle` family
- Filter table → `inet filter` family  
- Conntrack states → `ct state` instead of `-m conntrack`
- Marks → `meta mark set`
- IPset references → nftables `set` definitions

**Priority 2: Dynamic Blocklists** (dbl-setup.sh, dbl-cleanup.sh)

From:
```bash
ipset create dblsets hash:ip
iptables -A dynamic-block-list -m set --match-set dblsets dst -j DROP
```

To:
```nftables
set dblsets { type ipv4_addr; }
chain dynamic-block-list { 
    ip daddr @dblsets drop
}
```

**Priority 3: Application-Specific Scripts**
- WireGuard VPN: `/etc/untangle/iptables-rules.d/720-wireguard-vpn`
- OpenVPN: Generated iptables rules
- WAN Balancer: `/etc/untangle/iptables-rules.d/330-wan-balancer-rules`
- Intrusion Prevention: nfqueue and mangle rules

#### 2.2 Backward Compatibility Layer

Create wrapper that detects kernel version:
```bash
# New: ut-rules-update.sh (wrapper)
if [ "$(kernel_supports_nftables)" = "yes" ]; then
    /usr/lib/untangle/nftables-rules.sh
else
    /usr/lib/untangle/iptables-rules.sh
fi
```

---

### Phase 3: Java Application Migration (Weeks 5-7)

**Goal:** Update Java apps to generate nftables-compatible rules

#### 3.1 Core Infrastructure Updates

**File 1: uvm/impl/com/untangle/uvm/NetworkManagerImpl.java**
- Replace iptables command generation with nftables
- Update mark handling (still compatible)
- Modify rule application logic

```java
// Before
String cmd = "iptables -t mangle -A mark-src-intf -i " + iface + " -j MARK --set-mark 0xfa";
execManager.exec(cmd);

// After
String nftRule = "add rule inet mangle mark-src-intf iifname " + iface + " meta mark set 0xfa";
nftablesRulesManager.addRule(nftRule);
```

**File 2: uvm/impl/com/untangle/uvm/HostTableImpl.java**
- Update session mark application
- Handle traffic classification

**File 3: uvm/impl/com/untangle/uvm/NetFilterLogger.java**
- Update nfqueue interaction (if needed)

#### 3.2 Application-Level Updates

**WireGuardVpnManager.java (Lines 33, 76, 86, 113, 250-262)**

Current flow:
```java
private static final String IPTABLES_SCRIPT = "/etc/untangle/iptables-rules.d/720-wireguard-vpn";
configureIptables() {
    ExecManagerResult result = UvmContextFactory.context().execManager().exec(IPTABLES_SCRIPT);
}
```

New flow:
```java
private NftablesRulesManager nftRulesManager;
configureNftables() {
    String rules = generateWireGuardNftRules(this.settings);
    nftablesRulesManager.applyRules(rules);
}
```

**WanBalancerApp.java (Lines 215, 280, 332-358)**

Update rule generation for route marking in nftables:
```java
// Before: Writing shell script with iptables commands
FileUtils.writeFile(outputFile, "iptables -t mangle -A wan-route-rules ...");

// After: Generate nftables rule set
String nftRules = generateWanBalancerRules(this.settings);
nftablesRulesManager.applyRules(nftRules);
```

**Other Apps:**
- OpenVpnManager.java - VPN interface marking
- FtpNatHandler.java - NAT rule application
- IntrusionPreventionApp.java - nfqueue configuration

#### 3.3 Mark Handling (Unchanged at API level)

Marks are **NOT changing** - they're just applied via different mechanism:
```
Still using: 0xfa, 0xfa00, 0x01000000, etc.
Just applied via: "meta mark set 0xfa" instead of "iptables -j MARK --set-mark"
```

---

### Phase 4: Kernel & Libnetcap Updates (Weeks 8-9)

**Goal:** Ensure jnetcap/libnetcap work with nftables netfilter hooks

#### 4.1 Kernel Patch Migration

**Current Patches** → **nftables Equivalents:**

| Current Patch | Purpose | nftables Approach |
|---------------|---------|-------------------|
| 0002-extensions | NFMARK in cmsg | Keep as-is (kernel API, not userspace) |
| 0004-iptables-tune | Custom tune table | Create custom nftables chain hook |
| 0005-xt_mac | MAC byte matching | nftables packet register manipulation |
| 0006-iptables-socket | Socket mark restoration | nftables socket expression enhancement |

**Key Point:** Kernel patches remain mostly intact; they're kernel-level infrastructure not iptables-specific.

#### 4.2 libnetcap Verification

Review `jnetcap/libnetcap` interaction:
- Confirm nfqueue hooks still function with nftables
- Verify conntrack interaction (should be unchanged)
- Test packet marking and restoration

#### 4.3 libvector Compatibility

libvector receives processed traffic - should be independent of nftables vs iptables.

---

### Phase 5: Integration & Testing (Weeks 10-11)

**Goal:** Full system integration on Bookworm

#### 5.1 Build System Updates

- **rakefile**: Add Bookworm-specific builds
- **buildtools/**: Support nftables rule generation
- **debian/**: Update build profiles for nftables

#### 5.2 System Testing

**Unit Tests:**
- Rule generation correctness
- nftables syntax validation
- Mark preservation through pipeline

**Integration Tests:**
- Full firewall rule application
- Traffic flow verification
- QoS/marking functionality
- Dynamic blocklist operation
- VPN rule injection

**Hardware Tests:**
- Live traffic handling
- Session establishment
- Performance benchmarking vs iptables

#### 5.3 Fallback & Rollback

- Dual-boot capability (iptables on bullseye, nftables on bookworm)
- Automated fallback if nftables rules fail
- Clean uninstall/removal procedures

---

### Phase 6: Deployment & Documentation (Week 12)

**Goal:** Production-ready Bookworm support

#### 6.1 Migration Documentation

For developers:
- nftables rule syntax guide
- Migration checklist for new apps
- Testing procedures

For operators:
- Upgrade path from Bullseye→Bookworm
- Troubleshooting guide
- Performance expectations

#### 6.2 Deployment Checklist

- [ ] Kernel patches validated for nftables
- [ ] All 11 shell scripts migrated
- [ ] All 21 Java files updated
- [ ] Backward compatibility tested
- [ ] Performance validated
- [ ] Documentation complete
- [ ] CI/CD pipelines updated

---

## Part 4: Implementation Details by Component

### 4.1 Mark Handling (No Changes Needed)

**Example:** QoS Traffic Classification

**Current (iptables):**
```bash
iptables -t mangle -A mark-src-intf -i eth0 -j MARK --set-mark 0xfa
iptables -t mangle -A mark-traffic -p tcp --dport 80 -j MARK --set-mark 0x1000
```

**New (nftables):**
```nftables
chain mark-src-intf {
    iifname "eth0" meta mark set 0xfa
}
chain mark-traffic {
    tcp dport 80 meta mark set 0x1000
}
```

**Result:** Identical mark values applied, just different syntax

### 4.2 Session Tracking (Minor Changes)

**Current (iptables):**
```java
// Extract from /proc/net/nf_conntrack
readLine = "ipv4     2 tcp      6 123 ESTABLISHED src=10.0.0.1 dst=10.0.0.2 sport=443 dport=5000 src=10.0.0.2 dst=10.0.0.1 sport=5000 dport=443 [ASSURED] mark=0xfa00 zone=0 use=2"
```

**New (nftables):**
```java
// Query via nftables API (if available) or read same /proc interface
// Mark values remain: 0xfa00
ct mark == 0xfa00  // In nftables rule syntax
```

**Note:** conntrack integration remains largely unchanged

### 4.3 Dynamic Blocklists (Major Change)

**Current (iptables + ipset):**
```bash
# Create set
ipset create dblsets hash:ip

# Add IPs
ipset add dblsets 10.0.0.1

# Reference in iptables
iptables -A chain -m set --match-set dblsets dst -j DROP

# Remove dynamically
ipset del dblsets 10.0.0.1
```

**New (nftables):**
```nftables
# Define set
set dblsets { type ipv4_addr; flags dynamic; }

# Rule references set
chain dynamic-block-list {
    ip daddr @dblsets drop
}
```

**Java Addition (sync-settings → nftables API):**
```python
# In nftables_util.py or similar
def add_to_blocklist(ip_address):
    """Dynamically add IP to nftables set"""
    execute_cmd(f"nft add element inet filter dblsets {{ {ip_address} }}")

def remove_from_blocklist(ip_address):
    """Remove IP from set"""
    execute_cmd(f"nft delete element inet filter dblsets {{ {ip_address} }}")
```

---

## Part 5: Risk Assessment & Mitigation

### 5.1 High-Risk Areas

| Risk | Impact | Mitigation |
|------|--------|-----------|
| **Kernel patches incompatible** | Loss of core functionality (marks, tune table) | Early kernel patch review & testing |
| **Performance regression** | Network throughput/latency degradation | Benchmark early, optimize rules |
| **Rule atomicity** | Partial rule application, inconsistent state | Use nftables batch mode exclusively |
| **Third-party apps** | Unknown apps using iptables directly | Clear documentation, deprecation notice |
| **Backward compatibility** | Can't run on older kernels | Maintain dual code paths or separate branches |

### 5.2 Testing Strategy

**Layer 1: Unit Tests**
- Rule syntax generation
- Mark value correctness
- nftables API calls

**Layer 2: Integration Tests**
- Full rule set application
- Traffic routing verification
- Mark propagation through pipeline

**Layer 3: System Tests**
- Real traffic patterns
- Failover scenarios
- Performance under load

**Layer 4: Regression Tests**
- All existing Bullseye functionality on new codebase
- Compatibility matrix (Bullseye=iptables, Bookworm=nftables)

---

## Part 6: Deliverables & Timeline

### Timeline Summary
- **Week 1-2:** Foundation, tooling, Python abstraction
- **Week 3-4:** Shell script migration
- **Week 5-7:** Java application updates
- **Week 8-9:** Kernel & libnetcap verification
- **Week 10-11:** Integration & comprehensive testing
- **Week 12:** Finalization & documentation

### Key Deliverables

1. **Code Changes:**
   - `sync-settings/sync/nftables_rules_manager.py` (NEW)
   - Migrated shell scripts (11 files)
   - Updated Java files (21 files)
   - Kernel patch compatibility layer

2. **Documentation:**
   - Migration guide for developers
   - Operator's guide for Bookworm deployment
   - Architecture documentation

3. **Testing:**
   - Automated test suite
   - Performance benchmarks
   - Compatibility matrix

4. **Build System:**
   - Bookworm-specific build profiles
   - Conditional compilation paths
   - CI/CD updates

---

## Part 7: Success Criteria

✅ **Migration Complete When:**

1. All 21 Java files successfully generate nftables rules
2. All 11 shell scripts replaced with nftables equivalents
3. Backward compatibility maintained (iptables still works on Bullseye)
4. Kernel patches functional with nftables infrastructure
5. All unit, integration, and system tests pass
6. Performance metrics comparable to iptables version
7. Documentation complete and reviewed
8. No regression in firewall functionality
9. VPN, QoS, dynamic blocklists, and all apps operational
10. Deployment on Bookworm hardware verified

---

## Appendix A: File-by-File Migration List

### Shell Scripts (11 files)

1. `uvm/hier/usr/share/untangle/bin/ut-uvm-update-rules.sh` - MAIN
2. `dynamic-blocklists/hier/usr/share/untangle/bin/dbl-setup.sh`
3. `dynamic-blocklists/hier/usr/share/untangle/bin/dbl-cleanup.sh`
4. `tunnel-vpn/hier/usr/share/untangle/bin/tunnel-vpn-up.sh`
5. `tunnel-vpn/hier/usr/share/untangle/bin/tunnel-vpn-down.sh`
6. `uvm/hier/usr/share/untangle/bin/ut-routedump.sh` (minor)
7. Generated: `intrusion-prevention/hier/etc/untangle/iptables-rules.d/740-suricata`
8. Generated: `wireguard-vpn/iptables-rules.d/720-wireguard-vpn`
9. Generated: `wan-balancer/iptables-rules.d/330-wan-balancer-rules`
10. Generated: `openvpn/iptables-rules.d/XXX-openvpn-*`
11. Generated: Other app-specific rules

### Java Files (21 files)

**Core Infrastructure:**
1. `uvm/impl/com/untangle/uvm/NetworkManagerImpl.java`
2. `uvm/impl/com/untangle/uvm/HostTableImpl.java`
3. `uvm/impl/com/untangle/uvm/NetFilterLogger.java`
4. `uvm/impl/com/untangle/uvm/SessionTableImpl.java`

**VPN Applications:**
5. `wireguard-vpn/src/com/untangle/app/wireguard_vpn/WireGuardVpnManager.java`
6. `openvpn/src/com/untangle/app/openvpn/OpenVpnManager.java`
7. `ipsec-vpn/src/com/untangle/app/ipsec_vpn/IpsecVpnManager.java`
8. `tunnel-vpn/src/com/untangle/app/tunnel_vpn/TunnelVpnManager.java`

**Network Applications:**
9. `wan-balancer/src/com/untangle/app/wan_balancer/WanBalancerApp.java`
10. `router/src/com/untangle/app/router/RouterApp.java` (if iptables used)

**Protocol Casings:**
11. `ftp-casing/src/com/untangle/app/ftp/FtpNatHandler.java`

**Security Applications:**
12. `intrusion-prevention/src/com/untangle/app/intrusion_prevention/IntrusionPreventionApp.java`
13. `intrusion-prevention/src/com/untangle/app/intrusion_prevention/IntrusionPreventionSettings.java`
14. `dynamic-blocklists/src/com/untangle/app/dynamic_blocklists/DynamicBlockListsManager.java`
15. `shield/src/com/untangle/app/shield/ShieldApp.java`

**Configuration & API:**
16. `uvm/api/com/untangle/uvm/network/InterfaceSettings.java`
17. `uvm/api/com/untangle/uvm/app/ProtocolMatcher.java`
18. `uvm/api/com/untangle/uvm/app/RuleCondition.java`
19. `uvm/api/com/untangle/uvm/app/SessionEvent.java`

**Possibly affected:**
20. `uvm/impl/com/untangle/uvm/SessionMonitorImpl.java`
21. Other manager classes with rule generation

---

## Appendix B: nftables vs iptables Quick Reference

### Common Conversions

```
iptables → nftables

-t mangle        → table inet mangle
-A CHAIN         → add rule inet family chain
-j MARK --set    → meta mark set VALUE
-j ACCEPT        → accept
-j DROP          → drop
-j DNAT          → dnat to ADDRESS
-m conntrack     → ct state
-m set --match   → @setname (set lookup)
-i eth0          → iifname "eth0"
-o eth0          → oifname "eth0"
-p tcp --dport   → tcp dport
--state NEW      → ct state new
-m comment       → comment "text"
```

### Table/Chain Mapping

```
iptables               nftables
table filter    →    table inet filter
  chain INPUT         chain INPUT
  chain FORWARD       chain FORWARD
  chain OUTPUT        chain OUTPUT

table nat       →    table inet nat
table mangle    →    table inet mangle
```

---

## Appendix C: References & Resources

- **nftables Official:** https://wiki.nftables.org/
- **nftables Kernel:** https://kernelnewbies.org/Linux_4.18#Netfilter
- **Bookworm Release:** https://wiki.debian.org/DebianBookworm
- **Kernel Patches:** `/home/rohit/Arista-Workspace/Bookworm-Assemble/ngfw_kernels/debian-5.10.0/patches/untangle/`

---

**Document Status:** DRAFT - Ready for Architecture Review
**Last Updated:** January 29, 2026
**Prepared For:** Untangle NGFW Bookworm Migration Initiative
