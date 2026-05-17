variable "proxmox_endpoint" {
  type = string
}

variable "proxmox_ssh_address" {
  type        = string
  description = "SSH address of the Proxmox node used by the provider."
}

variable "target_node" {
  type = string
}

variable "ubuntu_image_file_id" {
  type        = string
  description = "Proxmox file id of the Ubuntu cloud image, for example local:iso/ubuntu-server.img."
}

variable "api_token" {
  type      = string
  sensitive = true
}

variable "pmx_root_password" {
  type      = string
  sensitive = true
}

variable "tskey_auth" {
  type      = string
  sensitive = true
}

variable "staging_ssh_pubkey_path" {
  type    = string
  default = "/root/.ssh/id_ed25519.pub"
}

variable "additional_ssh_pubkeys" {
  type      = list(string)
  sensitive = true
  default   = []
}

variable "vm_name_prefix" {
  type    = string
  default = "ai-detect-staging"
}

variable "vm_specs" {
  type = map(object({
    role        = string
    description = string
    cores       = number
    memory_mb   = number
    disk_gb     = number
  }))

  default = {
    manager = {
      role        = "manager"
      description = "Docker Swarm manager"
      cores       = 2
      memory_mb   = 4096
      disk_gb     = 50
    }
    backend = {
      role        = "backend"
      description = "Docker Swarm backend worker"
      cores       = 4
      memory_mb   = 6144
      disk_gb     = 80
    }
    ai = {
      role        = "ai"
      description = "Docker Swarm AI inference worker"
      cores       = 6
      memory_mb   = 12288
      disk_gb     = 120
    }
    monitoring = {
      role        = "monitoring"
      description = "Docker Swarm monitoring worker"
      cores       = 4
      memory_mb   = 8192
      disk_gb     = 120
    }
  }
}
