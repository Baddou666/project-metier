terraform {
    required_providers {
        proxmox = {
            source = "bpg/proxmox"
            version = "~> 0.104"
        }
    }
}
provider "proxmox" {
        endpoint = var.proxmox_endpoint
        api_token = var.api_token
        insecure = true

        ssh {
            agent = true
            username = "root"
            private_key = file("C:/Users/noadaz/.ssh/id_ed25519")
  }
    }
