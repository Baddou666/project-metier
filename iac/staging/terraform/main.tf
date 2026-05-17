data "local_file" "ssh_key" {
  filename = var.staging_ssh_pubkey_path
}

locals {
  ssh_key_content = sensitive(trimspace(data.local_file.ssh_key.content))
  ssh_key_valid   = can(regex("^ssh-", data.local_file.ssh_key.content))

  vm_definitions = {
    for name, spec in var.vm_specs : name => merge(spec, {
      hostname = "${var.vm_name_prefix}-${name}"
    })
  }
}

check "ssh_key_format" {
  assert {
    condition     = local.ssh_key_valid
    error_message = "Invalid SSH public key format"
  }
}

resource "proxmox_virtual_environment_file" "cloud_config" {
  for_each = local.vm_definitions

  content_type = "snippets"
  datastore_id = "local"
  node_name    = var.target_node

  source_raw {
    data = templatefile("${path.module}/cloud_config.yml.tpl", {
      tskey_auth         = var.tskey_auth
      hostname           = each.value.hostname
      node_role          = each.value.role
      staging_ssh_pubkey = data.local_file.ssh_key.content
      other_ssh_keys     = var.additional_ssh_pubkeys
    })
    file_name = "${each.value.hostname}-cloud-config.yaml"
  }
}

resource "proxmox_virtual_environment_vm" "swarm_node" {
  for_each = local.vm_definitions

  name        = each.value.hostname
  description = each.value.description
  node_name   = var.target_node
  tags        = ["ai-detector", "staging", "swarm", each.value.role]

  initialization {
    dns {
      servers = ["1.1.1.1", "8.8.8.8"]
    }

    ip_config {
      ipv4 {
        address = "dhcp"
      }
    }

    user_data_file_id = proxmox_virtual_environment_file.cloud_config[each.key].id
  }

  cpu {
    cores = each.value.cores
    type  = "host"
  }

  memory {
    dedicated = each.value.memory_mb
  }

  network_device {
    bridge = "vmbr0"
  }

  disk {
    datastore_id = "local-lvm"
    interface    = "scsi0"
    size         = each.value.disk_gb
    file_id      = var.ubuntu_image_file_id
  }

  serial_device {}
}