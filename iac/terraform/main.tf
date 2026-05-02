resource "proxmox_virtual_environment_file" "cloud_config" {
  content_type = "snippets"
  datastore_id = "local"
  node_name    = var.target_node

  source_raw {
    data = templatefile("${path.module}/cloud_config.yml.tpl", {
      tskey_auth = var.tskey-auth,
      hostname   = var.vm_name
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
        address = "10.10.50.135/24"
        gateway = "10.10.50.1"
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
