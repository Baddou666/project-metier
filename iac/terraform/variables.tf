variable "proxmox_endpoint" {
  type        = string
}

variable "username" {
  type        = string
}

variable "password" {
  type        = string
  sensitive = true
}

variable "vm_name" {
  type    = string
}

variable "target_node" {
  type    = string
}
variable api_token{
  type        = string
  sensitive = true
}