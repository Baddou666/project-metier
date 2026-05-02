terraform {
  required_providers {
    proxmox = {
      source  = "bpg/proxmox"
      version = "~> 0.104"
    }
  }
}

provider "proxmox" {
  endpoint  = var.proxmox_endpoint
  api_token = var.api_token
  insecure  = true

  ssh {
    username = "root"
    agent    = true

    node {
      name    = "pve"
      address = "pve.tail2c3ac8.ts.net"
    }
  }
}
