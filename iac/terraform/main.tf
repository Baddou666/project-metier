resource "proxmox_virtual_environment_file" "cloud_config"{
  content_type = "snippets"
  datastore_id = "local"
  node_name    = "fanida"

  source_raw {
    data = file("${path.module}/cloud_config.yaml")
    file_name = "cloud_config.yaml"
  }
}
resource "proxmox_virtual_environment_vm" "projet_metier_vm" {
  name        = var.vm_name
  description = "fully automated"
  node_name   = var.target_node
  initialization {

    ip_config {
      ipv4 {
        address = "192.168.11.93/24"
        gateway = "192.168.11.111"
      }
    }
    user_data_file_id = proxmox_virtual_environment_file.cloud_config.id
  }  
  cpu {
    cores = 4
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
    file_id      = "local:iso/ubuntu-server.img"
  }
  serial_device {}                       
  }
