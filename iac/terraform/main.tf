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
    user_account {
    username = "ubuntu"
    password = "ubuntu" # Plain text is okay here; the provider hashes it
    keys     = ["ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDvUY1VFgyVe1BZZ7oX2/lOXRLoubVUOGIo/VGTIi8S7 noadaz@DESKTOP-FF8H7R1","ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQCi68s3sos6unCs+oCJ5sBmkbFk/1VUDpvXEpoTy/1R/itti47qTHW2svLc7l+Ce2Fy5RTmhlYuAhQ5tPpr6SIbXib9LBveS9qVUI9ADby6b+3UvkgvAxzlJNXbHUkEzHxMMgvC0Qqnl2mxeV8Sb//zNSAbr0Jt3W9zVI6ZNXE53BS/FrGAQYM9xWHMbE3eWutSPKGNM87PtSJ7EHM5STHE7xYap4+xilH7wKxDurR8Ofigal8sOPEyFacCqbQvqf4E8fHTkVU9LBH3U8Q48y3CjU105PdyzF7XpJXXHMxs9pzZAQll7Oh5tfltCPHCK+kJibC1GW5TlTz9ewsGMOsQMMfHRQL0qNtLJVhJGwMsxfmL4UJAwIOdYyhLwVUYtdRmXTQW7BL1q/1yj/fv3qGq6MJfbR5+/nqilBe2xyA/Kx48fDJJl8JV6v97jQNW9jLBq636FyZvz9RSfStCEwxP0W6vxKRJK+c+JBt1jVGmeaYKafOSOlYixKXzys0iep75GC3CIklUAJNLEwm5KuxS21ggC2Gx3yRQiElQUBjeXmxiMgibndX+a3vRk8astqc/0aMV4DXIyZqM3GLaZmkAOwN7VIeEVu4o9a+N5B03RgS3xSKPWYM0O+xE/4YtxDND6H2Se8UkVH0e/og2bCNCR+2VookOMN/bw+9NlFKI9w== root@fanida"]
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
