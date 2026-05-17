output "swarm_nodes" {
  description = "Created Docker Swarm staging nodes."
  value = {
    for name, vm in proxmox_virtual_environment_vm.swarm_node : name => {
      hostname = vm.name
      role     = local.vm_definitions[name].role
      ipv4     = try(vm.ipv4_addresses, [])
    }
  }
}

output "ansible_inventory_hint" {
  description = "Inventory hostnames when Tailscale DNS is enabled."
  value = {
    manager = "${var.vm_name_prefix}-manager"
    backend = "${var.vm_name_prefix}-backend"
    ai      = "${var.vm_name_prefix}-ai"
    data    = "${var.vm_name_prefix}-data"
  }
}
