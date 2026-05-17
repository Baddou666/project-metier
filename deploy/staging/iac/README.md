# Staging IaC

This folder provisions and configures a four-node Docker Swarm staging cluster:

- `VM1`: Swarm manager
- `VM2`: backend worker
- `VM3`: AI worker
- `VM4`: data worker

## Terraform

Terraform creates the four Proxmox VMs with cloud-init and Tailscale enabled.

```sh
cd deploy/staging/iac/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

After apply, use the output hostnames in the Ansible inventory if you changed
`vm_name_prefix`.

## Ansible

Ansible installs Docker, initializes Swarm on the manager, joins the three
workers, and labels the nodes:

- `role=manager`
- `role=backend`
- `role=ai`
- `role=data`

```sh
cd deploy/staging/iac/ansible
ansible-playbook playbook.yml
```

Stack deployment is disabled by default. To enable it, create
`group_vars/all.yml` from `group_vars.example.yml` and set:

```yaml
deploy_stack: true
```
