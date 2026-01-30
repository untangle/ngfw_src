# Bookworm Migration: Implementation Roadmap & Team Assignments

## Project Overview

**Project Name**: Bullseye to Bookworm Migration (iptables → nftables)
**Duration**: 12 weeks (3 months)
**Target**: Full nftables support on Bookworm kernel 6.x
**Scope**: 21 Java files, 11 shell scripts, 8 kernel patches, 2 repositories

---

## Phase Breakdown & Deliverables

### Phase 1: Foundation & Abstraction (Weeks 1-2)

**Objective**: Create infrastructure for dual iptables/nftables support

**Deliverables:**

1. **nftables_rules_manager.py** (Python class in sync-settings)
   - `generate_mark_rules(interface, mark_value)`
   - `generate_filter_rules(protocol, port, action)`
   - `generate_nat_rules(src_ip, dst_ip)`
   - `execute_rules(rule_set)` - Atomic batch execution
   - Kernel version detection

2. **Kernel Patch Analysis Document**
   - Map 8 Bullseye patches to Bookworm equivalents
   - Identify which patches need modification
   - Document patch compatibility strategy

3. **Test Framework**
   - Unit tests for rule generation (iptables format)
   - Unit tests for rule generation (nftables format)
   - Syntax validators for nftables rules
   - Mock test environments

**Team Assignments:**
- **Lead**: Network Architecture (1 person)
  - Own: Python abstraction layer, kernel patch analysis
  - Coordinate: Test framework design
  
- **Support**: Build/Infrastructure (1 person)
  - Own: Test environment setup, CI/CD integration prep

**Success Criteria:**
- ✅ Abstraction layer supports both iptables and nftables
- ✅ All 8 kernel patches documented for Bookworm
- ✅ Test suite runs on both formats
- ✅ Kernel version auto-detection working

---

### Phase 2: Shell Script Migration (Weeks 3-4)

**Objective**: Replace 11 iptables shell scripts with nftables equivalents

**Deliverables:**

#### Priority 1 (Critical - Week 3)
1. **ut-uvm-update-rules.sh** (~230 lines)
   - Main rule orchestrator
   - Handles nfqueue configuration, mark application, redirect chains
   - NEW: `ut-uvm-update-rules-nftables.sh` + wrapper for auto-detection

2. **dbl-setup.sh / dbl-cleanup.sh**
   - Dynamic blocklist management
   - OLD: ipset-based rules
   - NEW: nftables set-based rules
   - Atomic add/remove operations

#### Priority 2 (High - Week 4)
3. **tunnel-vpn-up.sh / tunnel-vpn-down.sh**
   - VPN interface rules (mangle, POSTROUTING)
   - Interface marking for tunnel traffic

4. **Generated app scripts**
   - Wrapper infrastructure for auto-generation
   - WireGuard, OpenVPN, WAN-Balancer scripts

#### Priority 3 (Low - Ongoing)
5. **ut-routedump.sh** and diagnostic scripts
   - Minor changes, lower priority

**Technical Approach:**
```bash
# Wrapper script detects kernel and chooses path:
if [ "$(nft --version 2>/dev/null)" != "" ]; then
    /usr/lib/untangle/scripts/ut-uvm-update-rules-nftables.sh "$@"
else
    /usr/lib/untangle/scripts/ut-uvm-update-rules-iptables.sh "$@"
fi
```

**Team Assignments:**
- **Lead**: Shell Script Developer (1-2 people)
  - Own: Script rewrites, testing
  - Review: Functionality equivalence
  
- **Support**: QA (1 person)
  - Own: Script testing, comparison matrix
  - Verify: iptables vs nftables produce equivalent results

**Success Criteria:**
- ✅ All 11 scripts have nftables equivalents
- ✅ Auto-detection wrapper works
- ✅ Rules output identical firewall behavior
- ✅ Dynamic operations (add/remove IPs) work

---

### Phase 3: Java Application Updates (Weeks 5-7)

**Objective**: Update 21 Java files to support nftables rule generation

#### Week 5: Core Infrastructure (4 files)

**Files:**
1. `uvm/impl/com/untangle/uvm/NetworkManagerImpl.java` (Primary)
2. `uvm/impl/com/untangle/uvm/HostTableImpl.java`
3. `uvm/impl/com/untangle/uvm/NetFilterLogger.java`
4. `uvm/impl/com/untangle/uvm/SessionTableImpl.java`

**Changes:**
- Inject NftablesRulesManager dependency
- Replace `execManager.exec("iptables ...")` with nftables calls
- Kernel version detection
- Conditional rule application

**Example Pattern:**
```java
// Abstraction for both
interface RulesManager {
    void applyMark(String iface, int markValue);
    void addDynamicFilter(String ip, String action);
}

// Implementation selector
if (isNftablesSupported()) {
    rulesManager = new NftablesRulesManager();
} else {
    rulesManager = new IptablesRulesManager();
}

// Uniform usage
rulesManager.applyMark("eth0", 0xfa);
```

#### Week 6: VPN/Network Applications (6 files)

**Files:**
1. `wireguard-vpn/src/com/untangle/app/wireguard_vpn/WireGuardVpnManager.java`
2. `openvpn/src/com/untangle/app/openvpn/OpenVpnManager.java`
3. `ipsec-vpn/src/com/untangle/app/ipsec_vpn/IpsecVpnManager.java`
4. `tunnel-vpn/src/com/untangle/app/tunnel_vpn/TunnelVpnManager.java`
5. `wan-balancer/src/com/untangle/app/wan_balancer/WanBalancerApp.java`
6. `router/src/com/untangle/app/router/RouterApp.java` (if applicable)

**Changes:**
- Generate nftables rules instead of shell scripts
- Replace file-based rule storage with nftables API calls
- Keep mark values unchanged (0xfa, 0x01000000, etc.)

**Example:**
```java
// OLD
String script = "#!/bin/bash\niptables -t mangle ...";
FileUtils.write("/etc/untangle/iptables-rules.d/720-wireguard", script);
execManager.exec(script);

// NEW
NftablesRulesManager nftMgr = getNftablesManager();
String rules = generateWireGuardRules(config);
nftMgr.loadRuleSet(rules);  // Atomic batch load
```

#### Week 7: Protocol/Security Applications (5 files)

**Files:**
1. `ftp-casing/src/com/untangle/app/ftp/FtpNatHandler.java` (NAT rules)
2. `intrusion-prevention/src/com/untangle/app/intrusion_prevention/IntrusionPreventionApp.java`
3. `intrusion-prevention/src/com/untangle/app/intrusion_prevention/IntrusionPreventionSettings.java`
4. `dynamic-blocklists/src/com/untangle/app/dynamic_blocklists/DynamicBlockListsManager.java` (CRITICAL)
5. `shield/src/com/untangle/app/shield/ShieldApp.java`

**Special Attention:**
- **DynamicBlockListsManager**: Replace `ipset` with nftables `set` operations
- **IntrusionPreventionSettings**: Update queue number references (still used)
- **FtpNatHandler**: Convert inline iptables commands to nftables

**Team Assignments:**
- **Lead**: Java Core Dev (1 person)
  - Own: Core infrastructure (Week 5)
  - Review: All Java changes
  
- **Developer 2**: Java App Dev (1 person)
  - Own: VPN/Network apps (Week 6)
  - Test: Rule generation correctness
  
- **Developer 3**: Java App Dev (1 person)
  - Own: Protocol/Security apps (Week 7)
  - Special focus: DynamicBlockListsManager

- **Support**: QA (1 person)
  - Own: Unit testing, integration testing
  - Verify: No functional regression

**Success Criteria:**
- ✅ All 21 Java files updated
- ✅ Marks preserved (same values)
- ✅ Rules apply atomically via nftables
- ✅ Dynamic updates work (blocklists, VPN rules)
- ✅ No functional regression

---

### Phase 4: Kernel & libnetcap Verification (Weeks 8-9)

**Objective**: Verify kernel patches and network pipeline compatibility

**Deliverables:**

1. **Kernel Patch Verification Report**
   - Test each of 8 patches on Bookworm kernel 6.x
   - Document any modifications needed
   - Compatibility matrix: Bullseye ✓, Bookworm ✓/✗

2. **libnetcap Integration Test**
   - Verify nfqueue hooks still function
   - Test conntrack interaction
   - Validate packet marking propagation

3. **Performance Baseline**
   - Throughput comparison (iptables vs nftables)
   - Latency measurements
   - CPU utilization

**Technical Steps:**

```bash
# Week 8: Kernel testing
cd /home/rohit/Arista-Workspace/Bookworm-Assemble/ngfw_kernels/debian-5.10.0/patches
for patch in untangle/*.patch; do
    patch --dry-run < $patch
    # Document: succeeds, conflicts, modifications needed
done

# Week 9: Integration testing
# Boot Bookworm system with patches applied
# Generate nftables rules
# Verify: packets marked correctly, traffic flows, performance acceptable
```

**Team Assignments:**
- **Lead**: Kernel/Systems Engineer (1 person)
  - Own: Kernel patch verification
  - Coordinate: Integration testing on hardware
  
- **Support**: Performance Engineer (1 person)
  - Own: Performance benchmarking
  - Compare: iptables vs nftables metrics

**Success Criteria:**
- ✅ All 8 kernel patches verified/adapted for Bookworm
- ✅ nfqueue + conntrack working with nftables
- ✅ Packet marks propagate correctly
- ✅ Performance acceptable (≥95% of iptables baseline)

---

### Phase 5: Integration & Comprehensive Testing (Weeks 10-11)

**Objective**: Full system validation across test environments

#### Week 10: Integration Testing

**Test Environments:**

1. **Bullseye VM** (Regression/Backward Compat)
   - iptables rules still work
   - All apps operational
   - Performance baseline maintained

2. **Bookworm VM** (nftables)
   - All nftables rules apply correctly
   - All apps operational
   - Feature parity verified

3. **Hardware Test** (if available)
   - Real traffic patterns
   - Live VPN connectivity
   - Dynamic blocklist updates

**Test Scenarios:**

```
Traffic Flow Tests:
├─ Session creation (TCP/UDP)
├─ Port forwarding (FTP, HTTP, HTTPS)
├─ VPN rule injection (WireGuard, OpenVPN)
├─ QoS marking propagation
└─ Dynamic blocking (adding/removing IPs)

Firewall Rules Tests:
├─ Mangle chain (marking)
├─ Filter chain (drop/accept)
├─ NAT chain (DNAT/SNAT)
├─ Chain jumps (to custom chains)
└─ Set operations (add/remove members)

Performance Tests:
├─ Throughput (Mbps with various loads)
├─ Latency (first packet, established sessions)
├─ CPU utilization (single/multi-threaded)
└─ Memory usage (rule count scaling)

Edge Cases:
├─ Many simultaneous connections
├─ Large blocklists (100K+ IPs)
├─ Rapid rule updates (add/remove quickly)
├─ System restart (persistence)
└─ Kernel panic recovery
```

#### Week 11: Regression & Finalization

**Activities:**
1. Comprehensive regression suite (Bullseye backward compat)
2. Resolve any issues found in Week 10
3. Performance optimization if needed
4. Documentation finalization
5. Go/No-go decision for production

**Test Metrics:**
- Test Pass Rate: Target 100%
- Regression Issues: 0 critical, ≤2 minor
- Performance Delta: ±5% vs iptables
- Code Coverage: ≥90% for rule generation

**Team Assignments:**
- **Lead**: QA/Test Engineer (1 person)
  - Own: Test planning, execution, reporting
  
- **Developer 1-3**: Developers on-call
  - Own: Issue triage and fixes
  - Support: Test execution
  
- **Performance Engineer**: Hardware testing
  - Own: Performance validation
  - Compare: iptables vs nftables baselines

**Success Criteria:**
- ✅ All integration tests pass
- ✅ Bullseye backward compatibility verified
- ✅ No regressions in functionality
- ✅ Performance acceptable
- ✅ Ready for production deployment

---

### Phase 6: Documentation & Deployment (Week 12)

**Objective**: Finalize documentation and prepare for production

**Deliverables:**

1. **Architecture Documentation** (2-3 pages)
   - nftables rule structure for NGFW
   - Mark handling explanation
   - Dynamic update mechanisms
   - Kernel hook interaction

2. **Developer Guide** (5-10 pages)
   - When to use iptables vs nftables
   - Code patterns for rule generation
   - Common conversions reference
   - Troubleshooting guide

3. **Operator Guide** (5-10 pages)
   - Bullseye to Bookworm upgrade path
   - Troubleshooting with `nft` commands
   - Performance expectations
   - Rollback procedures

4. **API Reference** (Appendix)
   - NftablesRulesManager class documentation
   - Python nftables_rules_manager documentation
   - Shell script interfaces

5. **Migration Checklist**
   - Pre-deployment verification
   - Deployment steps
   - Post-deployment validation
   - Rollback procedures

6. **CI/CD Updates**
   - Build profiles for Bullseye (iptables) and Bookworm (nftables)
   - Automated testing for both
   - Kernel detection in build system

**Team Assignments:**
- **Lead**: Technical Writer (1 person)
  - Own: All documentation
  - Coordinate: Developer input
  
- **Developers**: Input/Review (as needed)
  - Provide: Technical accuracy, examples
  - Review: Documentation correctness

**Success Criteria:**
- ✅ All documentation complete and reviewed
- ✅ Deployment checklist finalized
- ✅ CI/CD pipelines updated
- ✅ Team trained on new procedures

---

## Overall Team Structure

```
Steering Committee
├─ Project Manager (1)
│  └─ Timeline, resource allocation, risk management
│
├─ Technical Lead (1)
│  └─ Architecture decisions, code reviews
│
└─ Team Members
   ├─ Network/Core Developer (1)
   │  └─ Phase 1-3 (Foundation, Infrastructure, Java)
   │
   ├─ Shell Script Developer (1-2)
   │  └─ Phase 2 (Script migration)
   │
   ├─ Java Application Developers (2-3)
   │  └─ Phase 3 (Java updates)
   │
   ├─ Kernel/Systems Engineer (1)
   │  └─ Phase 4 (Kernel verification)
   │
   ├─ QA/Test Engineer (1-2)
   │  └─ Phase 5 (Integration testing)
   │
   └─ Technical Writer (1)
      └─ Phase 6 (Documentation)
```

**Total Team**: 8-10 people
**Average Load**: 1-2 people per week
**Peak Load**: Week 3-5, 10-11 (3-4 people)

---

## Risk Register

| # | Risk | Probability | Impact | Mitigation |
|---|------|-------------|--------|-----------|
| 1 | Kernel patches incompatible | Low | Critical | Early verification (Week 8), maintain fallback |
| 2 | Performance regression | Low-Med | High | Early benchmarking, optimization buffer in timeline |
| 3 | Rule atomicity issues | Low | High | Use nftables batch mode exclusively, comprehensive testing |
| 4 | Third-party app breakage | Med | Med | Code review, forced abstraction layer, clear deprecation |
| 5 | Backward compat breaks | Low | High | Regression test suite, dual build paths |
| 6 | Schedule slip | Med | Low | Aggressive Phase 1, parallel work in Phase 3 |
| 7 | Team knowledge gap | Med | Med | Early training, documentation, peer reviews |

---

## Success Metrics

### Functional
- [ ] All 21 Java files updated
- [ ] All 11 shell scripts converted
- [ ] 0 critical bugs in integration testing
- [ ] 100% feature parity vs iptables version
- [ ] VPN, QoS, blocklists all operational

### Performance
- [ ] Throughput ±5% of iptables baseline
- [ ] Latency ±5% of iptables baseline
- [ ] CPU utilization ≤110% of iptables baseline
- [ ] Rule application time <100ms

### Quality
- [ ] Code coverage ≥90% (rule generation)
- [ ] 0 security regressions
- [ ] Documentation >95% complete
- [ ] Team proficiency verified via training

### Deployment
- [ ] Bullseye backward compatibility verified
- [ ] Bookworm hardware deployment successful
- [ ] Rollback procedures tested
- [ ] Operator readiness confirmed

---

## Timeline Summary

```
Week 1-2:   Phase 1 - Foundation (Python abstraction, test framework)
Week 3-4:   Phase 2 - Shell scripts (11 scripts migrated)
Week 5-7:   Phase 3 - Java updates (21 files, all apps)
Week 8-9:   Phase 4 - Kernel verification (patches, libnetcap)
Week 10-11: Phase 5 - Integration testing (comprehensive validation)
Week 12:    Phase 6 - Documentation & deployment readiness

Total: 12 weeks (3 months)
Go-Live: Week 13+ (production deployment)
```

---

## Approval & Sign-Off

**Document Prepared By**: Architecture & Planning
**Date**: January 29, 2026
**Status**: READY FOR APPROVAL

**Required Approvals:**
- [ ] Project Manager
- [ ] Technical Lead
- [ ] QA Lead
- [ ] DevOps/Infra Lead

**Notes for Approval:**
1. Confirm team availability for 12 weeks
2. Approve budget for Bookworm test environment
3. Assign Project Manager and Technical Lead
4. Schedule kickoff meeting (Phase 1 start)

---

## Appendix: Detailed File List

See `BOOKWORM_NFTABLES_MIGRATION_PLAN.md` Appendix C for complete file inventory.
