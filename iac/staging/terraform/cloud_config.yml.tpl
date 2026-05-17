#cloud-config
hostname: ${hostname}
manage_etc_hosts: true
users:
  - default
  - name: ubuntu
    groups:
      - sudo
    shell: /bin/bash
    lock_passwd: true
    ssh_authorized_keys:
%{for key in concat([staging_ssh_pubkey], other_ssh_keys) ~}
      - ${key}
%{endfor ~}
    sudo: ALL=(ALL) NOPASSWD:ALL
ssh_pwauth: false
disable_root: true
write_files:
  - path: /etc/ssh/sshd_config.d/99-cloud-init-hardening.conf
    owner: root:root
    permissions: '0644'
    content: |
      PasswordAuthentication no
      KbdInteractiveAuthentication no
      ChallengeResponseAuthentication no
      PubkeyAuthentication yes
      PermitRootLogin no
  - path: /etc/ai-detector-node-role
    owner: root:root
    permissions: '0644'
    content: |
      ${node_role}
packages:
  - curl
runcmd:
  - systemctl reload ssh || systemctl reload sshd
  - bash -lc 'curl -fsSL https://tailscale.com/install.sh | sh && sudo tailscale up --auth-key=${tskey_auth} --hostname=${hostname}'
