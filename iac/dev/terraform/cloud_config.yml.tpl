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
      - ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIDvUY1VFgyVe1BZZ7oX2/lOXRLoubVUOGIo/VGTIi8S7 noadaz@DESKTOP-FF8H7R1
      - ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQCi68s3sos6unCs+oCJ5sBmkbFk/1VUDpvXEpoTy/1R/itti47qTHW2svLc7l+Ce2Fy5RTmhlYuAhQ5tPpr6SIbXib9LBveS9qVUI9ADby6b+3UvkgvAxzlJNXbHUkEzHxMMgvC0Qqnl2mxeV8Sb//zNSAbr0Jt3W9zVI6ZNXE53BS/FrGAQYM9xWHMbE3eWutSPKGNM87PtSJ7EHM5STHE7xYap4+xilH7wKxDurR8Ofigal8sOPEyFacCqbQvqf4E8fHTkVU9LBH3U8Q48y3CjU105PdyzF7XpJXXHMxs9pzZAQll7Oh5tfltCPHCK+kJibC1GW5TlTz9ewsGMOsQMMfHRQL0qNtLJVhJGwMsxfmL4UJAwIOdYyhLwVUYtdRmXTQW7BL1q/1yj/fv3qGq6MJfbR5+/nqilBe2xyA/Kx48fDJJl8JV6v97jQNW9jLBq636FyZvz9RSfStCEwxP0W6vxKRJK+c+JBt1jVGmeaYKafOSOlYixKXzys0iep75GC3CIklUAJNLEwm5KuxS21ggC2Gx3yRQiElQUBjeXmxiMgibndX+a3vRk8astqc/0aMV4DXIyZqM3GLaZmkAOwN7VIeEVu4o9a+N5B03RgS3xSKPWYM0O+xE/4YtxDND6H2Se8UkVH0e/og2bCNCR+2VookOMN/bw+9NlFKI9w== root@fanida
      - ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAICap8iQpRoFnxn5ijnGV864vhoVZs9KL3R6pZ1G78aq3 baddou.ayman@ensam-casa.ma
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
packages:
  - curl
runcmd:
  - systemctl reload ssh || systemctl reload sshd
  - bash -lc 'curl -fsSL https://tailscale.com/install.sh | sh && sudo tailscale up --auth-key=${tskey_auth}'
