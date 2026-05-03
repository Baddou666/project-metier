variable "proxmox_endpoint" {
  type = string
}

variable "vm_name" {
  type = string
}

variable "target_node" {
  type = string
}

variable "ubuntu_image_file_id" {
  type = string
}

variable "api_token" {
  type      = string
  sensitive = true
}

variable "tskey-auth" {
  type      = string
  sensitive = true
}
