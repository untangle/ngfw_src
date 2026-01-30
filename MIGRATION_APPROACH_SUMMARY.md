# Bookworm Migration: Suggested Plan & Approach

## Executive Summary

You're migrating NGFW from **Bullseye (iptables)** to **Bookworm (nftables)** due to Bullseye EOL and kernel 6.x deprecation of iptables. This document outlines the high-level strategy.

---

## Current Situation

### Scope of iptables Usage

| Category | Count | Location |
|----------|-------|----------|
| Java Files with iptables refs | 21 files | 152 references total |
| Shell scripts with iptables | 11 scripts | 132 references total |
| Kernel patches needed | 8 patches | Mostly infrastructure (keep) |
| Custom firewall tables | 3-4 tables | `mangle`, `filter`, `nat`, + custom `tune` |

### Critical Infrastructure

**Network Stack Pipeline:**
```
Linux Kernel (nfqueue/conntrack)
    ↓ [jnetcap + libnetcap - C library]
    ↓ [nftables rules instead of iptables]
    ↓ [Java apps via UvmContext managers]
    ↓ [Applications: firewall, threat-prevention, VPN, QoS]
```

**Key Apps That Generate Rules:**
- WireGuardVpnManager (VPN rules)
- OpenVpnManager (VPN rules)
- WanBalancerApp (Traffic routing)
- FtpNatHandler (NAT redirection)
- DynamicBlockListsManager (Dynamic IPs)
- IntrusionPreventionApp (Suricata integration)

---

## Why nftables (and why it matters)

### Problem with iptables on Kernel 6.x

- Kernel 6.x **deprecated** iptables userspace tools
- nftables is the **official replacement** (also more efficient)
- Can't use imperative iptables commands (`iptables -A`, `-D`, etc.) on newer kernels
- Need **declarative, atomic rule sets** instead

### nftables Advantages

| Feature | iptables | nftables |
|---------|----------|----------|
| **Syntax** | Multiple tools, imperative | Single `nft` command, declarative |
| **Atomicity** | Per-rule (inconsistent states possible) | Batch atomic (consistent) |
| **Performance** | Linear rule traversal | Optimized lookup with sets/maps |
| **State** | In-memory, lost on reboot | Can be defined in files |
| **Maintenance** | Complex shell scripts | Cleaner rule syntax |

---

## Proposed Approach (12-Week Phased Plan)

### Phase 1: Foundation & Abstraction Layer (Weeks 1-2)

**Goal:** Create unified rule generation framework

**Action Items:**
1. **Create Python Abstraction** (`sync-settings/sync/nftables_rules_manager.py`)
   - Single interface for both iptables (Bullseye) and nftables (Bookworm)
   - Methods: `generate_mark_rules()`, `generate_filter_rules()`, `execute_rules()`
   - Detects kernel version and applies correct syntax
   
2. **Document Kernel Patches**
   - Map each Bullseye patch to nftables equivalent
   - 0002-extensions: Keep (kernel-level NFMARK in cmsg)
   - 0004-iptables-tune: Create custom nftables chain
   - 0005-xt_mac: nftables packet manipulation
   - 0006-iptables-socket: nftables socket expression

3. **Build Test Suite**
   - Unit tests for rule generation (both formats)
   - Syntax validators for nftables

**Key Decision:** Support **both iptables and nftables** in code (backward compatibility)

---

### Phase 2: Shell Script Migration (Weeks 3-4)

**Goal:** Replace iptables shell scripts with nftables equivalents

**11 Scripts to Migrate:**

**Priority 1 (Critical):**
- `ut-uvm-update-rules.sh` (~230 lines) - Main orchestrator
- `dbl-setup.sh` + `dbl-cleanup.sh` - Dynamic blocklists

**Priority 2 (High):**
- `tunnel-vpn-up.sh` / `tunnel-vpn-down.sh` - VPN integration
- App-generated scripts (WireGuard, OpenVPN, WAN-Balancer)

**Priority 3 (Low):**
- `ut-routedump.sh` - Diagnostic (minor changes)

**Conversion Example:**

```bash
# OLD (iptables)
iptables -t mangle -A mark-src-intf -i eth0 -j MARK --set-mark 0xfa
iptables -A dynamic-block-list -m set --match-set dblsets dst -j DROP

# NEW (nftables)
nft add table inet mangle
nft add chain inet mangle mark-src-intf "{ type route hook input priority 0; }"
nft add rule inet mangle mark-src-intf iifname eth0 meta mark set 0xfa

set dblsets { type ipv4_addr; }
chain dynamic-block-list { ip daddr @dblsets drop }
```

**Strategy:**
- Create wrapper script that detects kernel version
- Old path: `ut-uvm-update-rules-iptables.sh` (Bullseye)
- New path: `ut-uvm-update-rules-nftables.sh` (Bookworm)
- Dispatcher: `ut-uvm-update-rules.sh` calls correct version

---

### Phase 3: Java Application Updates (Weeks 5-7)

**Goal:** Update 21 Java files to generate nftables rules

**Core Infrastructure (Priority 1):**
1. `NetworkManagerImpl.java` - Rule generation logic
2. `HostTableImpl.java` - Session marks
3. `NetFilterLogger.java` - nfqueue interaction
4. `SessionTableImpl.java` - Session tracking

**Example Change:**

```java
// BEFORE
private void configureIptables() {
    String cmd = "iptables -t mangle -A mark-src-intf -i " + iface + 
                 " -j MARK --set-mark 0xfa";
    UvmContextFactory.context().execManager().exec(cmd);
}

// AFTER
private void configureNftables() {
    String nftRule = "add rule inet mangle mark-src-intf iifname " + 
                     iface + " meta mark set 0xfa";
    NftablesRulesManager.getInstance().addRule(nftRule);
}

// UNIFIED (supports both)
private void configureRules() {
    if (isNftablesSupported()) {
        configureNftables();
    } else {
        configureIptables();
    }
}
```

**VPN/App Updates (Priority 2):**
- WireGuardVpnManager.java - Generate nftables rules instead of shell scripts
- WanBalancerApp.java - Route marking via nftables
- OpenVpnManager.java - Interface marking
- FtpNatHandler.java - NAT rules

**Key Point:** Marks remain unchanged (0xfa, 0x01000000, etc.)—only **syntax** changes

---

### Phase 4: Kernel & libnetcap Verification (Weeks 8-9)

**Goal:** Ensure network pipeline works with nftables

**What Needs Checking:**
1. **Kernel Patches** - Do they work with nftables?
   - Most should work (they're kernel infrastructure, not userspace iptables)
   - Test on Bookworm kernel 6.x
   
2. **jnetcap Integration** - Does nfqueue still work?
   - jnetcap/libnetcap interact with kernel netfilter hooks (unchanged API)
   - Just different rule syntax in userspace
   
3. **Packet Marking** - Are marks still applied correctly?
   - Same mark values, same propagation through pipeline
   - Just applied via different command: `meta mark set` vs `-j MARK --set-mark`

**Testing:**
- Boot Bookworm system
- Generate test nftables rules
- Verify packets are marked correctly
- Check session tracking via conntrack

---

### Phase 5: Integration & Testing (Weeks 10-11)

**Goal:** Comprehensive system validation

**Test Levels:**

1. **Unit Tests**
   - Rule generation correctness
   - nftables syntax validation
   - Mark value preservation

2. **Integration Tests**
   - Full firewall rule application
   - Traffic flow through all chains
   - Dynamic rule updates (blocklists)

3. **System Tests**
   - Live traffic scenarios
   - VPN rule injection
   - QoS/marking functionality
   - Performance (throughput, latency)

4. **Regression Tests**
   - Bullseye (iptables) still works
   - No feature loss on Bookworm
   - Clean upgrade path

**Success Criteria:**
- All 11 scripts produce equivalent firewall rules
- All 21 Java files updated
- Traffic flows correctly through pipeline
- Performance comparable to iptables
- Backward compatibility maintained

---

### Phase 6: Documentation & Deployment (Week 12)

**Deliverables:**

1. **Developer Guide**
   - When to use iptables vs nftables rules
   - How to add new rules
   - Common conversions (reference table)

2. **Operator Guide**
   - Bullseye → Bookworm upgrade path
   - Troubleshooting with `nft list ruleset`
   - Performance expectations

3. **Architecture Doc**
   - nftables rule structure for NGFW
   - Mark handling explanation
   - Dynamic updates (blocklists, VPN)

4. **CI/CD Updates**
   - Build both Bullseye (iptables) and Bookworm (nftables)
   - Automated testing for both
   - Kernel detection in build system

---

## Key Technical Decisions

### 1. **Mark Handling: No Change**
Marks (0xfa, 0x01000000, etc.) are unchanged.
```
iptables: -j MARK --set-mark 0xfa
nftables: meta mark set 0xfa
```
Same values, same propagation—just different command syntax.

### 2. **Backward Compatibility: Required**
Support both iptables (Bullseye) and nftables (Bookworm):
```java
if (kernelSupportsNftables()) {
    applyNftablesRules();
} else {
    applyIptablesRules();
}
```

### 3. **Dynamic Updates: Use nftables Sets**
```bash
# OLD: ipset add dblsets 10.0.0.1
# NEW: nft add element inet filter dblsets { 10.0.0.1 }
```

### 4. **Atomicity: Batch Mode**
```bash
# Write all rules to file, then load atomically:
nft -f /tmp/rules.nft
# Not individual: nft add rule ...
```

### 5. **Kernel Patches: Mostly Unchanged**
Patches 0002, 0004, 0005, 0006 are infrastructure—verify compatibility, but likely keep as-is.

---

## Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|------------|-----------|
| Kernel patches break | Low | Early testing on Bookworm kernel 6.x |
| Performance regression | Low-Med | Benchmark early, optimize nftables rules |
| Rule application fails | Low | Use atomic batch mode, comprehensive testing |
| Apps use iptables directly | Medium | Code review, force abstraction layer |
| Backward compat breaks | Low | Comprehensive regression suite |

---

## Success Criteria Checklist

When complete, verify:

- [ ] All 21 Java files updated for nftables
- [ ] All 11 shell scripts converted/rewritten
- [ ] Backward compatibility maintained (iptables still works)
- [ ] Kernel patches verified on Bookworm 6.x
- [ ] All unit tests pass (both iptables & nftables)
- [ ] Integration tests pass (full system)
- [ ] Performance benchmarked (equal or better)
- [ ] Documentation complete
- [ ] CI/CD pipelines updated
- [ ] Deployment tested on hardware
- [ ] No regression in firewall functionality
- [ ] VPN, QoS, blocklists all operational

---

## Timeline

```
Week 1-2:   Foundation & Python abstraction layer
Week 3-4:   Shell script migration (11 scripts)
Week 5-7:   Java updates (21 files)
Week 8-9:   Kernel & libnetcap verification
Week 10-11: Comprehensive testing & integration
Week 12:    Documentation, finalization, deployment

Total: 12 weeks (3 months)
```

---

## Resources

- **Full Plan**: See `BOOKWORM_NFTABLES_MIGRATION_PLAN.md` (comprehensive 50-page doc)
- **Kernel Patches**: `/home/rohit/Arista-Workspace/Bookworm-Assemble/ngfw_kernels/debian-5.10.0/patches/untangle/`
- **nftables Wiki**: https://wiki.nftables.org/
- **Bookworm Info**: https://wiki.debian.org/DebianBookworm

---

## Next Steps

1. **Approve this approach** - Confirm 12-week phased plan is acceptable
2. **Assign resources** - Identify team members for each phase
3. **Start Phase 1** - Create Python abstraction layer
4. **Set up testing environment** - Bookworm VM with kernel 6.x
5. **Begin kernel patch review** - Understand compatibility requirements

---

**Document Status**: READY FOR APPROVAL
**Date**: January 29, 2026
**Prepared By**: Architecture & Planning
**For**: Untangle NGFW Bookworm Migration Initiative
