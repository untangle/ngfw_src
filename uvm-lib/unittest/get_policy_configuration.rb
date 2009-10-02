policy_manager = Untangle::RemoteUvmContext.policyManager()
pc = policy_manager.getPolicyConfiguration()

puts pc.inspect

puts policy_manager.getUserPolicyRules().inspect

