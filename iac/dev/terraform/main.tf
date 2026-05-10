data "local_file" "ssh_key" {
  filename = var.devbox-ssh-pubkey-path
}

locals {
  ssh_key_content = sensitive(trimspace(data.local_file.ssh_key.content))
  ssh_key_valid = can(regex("^ssh-", data.local_file.ssh_key.content))
}

check "ssh_key_format" {
  assert {
    condition     = local.ssh_key_valid
    error_message = "Invalid SSH public key format"
  }
}


resource "proxmox_virtual_environment_file" "cloud_config" {
  content_type = "snippets"
  datastore_id = "local"
  node_name    = var.target_node

  source_raw {
    data = templatefile("${path.module}/cloud_config.yml.tpl", {
      tskey_auth = var.tskey-auth,
      hostname   = var.vm_name,
      devbox_ssh_pubkey = data.local_file.ssh_key.content,
      other_ssh_keys = var.additionnal-ssh-pubkeys
    })
    file_name = "cloud_config.yaml"
  }
}
resource "proxmox_virtual_environment_vm" "projet_metier_vm" {
  name        = var.vm_name
  description = "created vm with terraform"
  node_name   = var.target_node
  initialization {
    dns {
      servers = ["1.1.1.1"]
    }

    ip_config {
      ipv4 {
        address = "dhcp"
      }
    }

    user_data_file_id = proxmox_virtual_environment_file.cloud_config.id
  }
  cpu {
    cores = 4
    type  = "host"
  }

  memory {
    dedicated = 6144
  }

  network_device {
    bridge = "vmbr0"
  }

  disk {
    datastore_id = "local-lvm"
    interface    = "scsi0"
    size         = 100
    file_id      = var.ubuntu_image_file_id
  }
  serial_device {}
}
