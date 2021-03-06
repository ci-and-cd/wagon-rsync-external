# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure("2") do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://vagrantcloud.com/search.
  # https://github.com/geerlingguy/packer-ubuntu-1804
  config.vm.box = "topinfra/ubuntu1804-ci"

  # Disable automatic box update checking. If you disable this, then
  # boxes will only be checked for updates when the user runs
  # `vagrant box outdated`. This is not recommended.
  config.vm.box_check_update = false

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine. In the example below,
  # accessing "localhost:8080" will access port 80 on the guest machine.
  # NOTE: This will enable public access to the opened port
  # config.vm.network "forwarded_port", guest: 80, host: 8080

  #rm -Rf .vagrant/
  #vagrant global-status
  #vagrant global-status --prune
  # config.vm.network "forwarded_port", guest: 6379, host: 6379, id: "SENTINEL_MASTER_PORT", auto_correct: true
  # config.vm.network "forwarded_port", guest: 26379, host: 26379, id: "SENTINEL_PORT", auto_correct: true
  # config.vm.network "forwarded_port", guest: 6381, host: 6381, id: "SENTINEL_SLAVE_PORT", auto_correct: true

  # Create a forwarded port mapping which allows access to a specific port
  # within the machine from a port on the host machine and only allow access
  # via 127.0.0.1 to disable public access
  # config.vm.network "forwarded_port", guest: 80, host: 8080, host_ip: "127.0.0.1"

  # Create a private network, which allows host-only access to the machine
  # using a specific IP.
  config.vm.network "private_network", ip: "192.168.33.20"

  # Modify Vagrant VM’s Default SSH PORT
  config.vm.network :forwarded_port, guest: 22, host: 2222, id: "ssh", disabled: true
  config.vm.network :forwarded_port, guest: 22, host: 2230, auto_correct: true
  config.ssh.port = 2230

  config.vm.network :forwarded_port, guest: 873, host: 2873, id: "rsync"
  config.vm.network :forwarded_port, guest: 5005, host: 25005, id: "remote_debug"

  # Create a public network, which generally matched to bridged network.
  # Bridged networks make the machine appear as another physical device on
  # your network.
  # config.vm.network "public_network"

  # use default ~/.vagrant.d/insecure_private_key
  # `vagrant ssh` or `ssh -i ~/.vagrant.d/insecure_private_key vagrant@192.168.33.10`
  config.ssh.insert_key = false

  # Share an additional folder to the guest VM. The first argument is
  # the path on the host to the actual folder. The second argument is
  # the path on the guest to mount the folder. And the optional third
  # argument is a set of non-required options.
  # config.vm.synced_folder "../data", "/vagrant_data"

  # Provider-specific configuration so you can fine-tune various
  # backing providers for Vagrant. These expose provider-specific options.
  # Example for VirtualBox:
  #
  # config.vm.provider "virtualbox" do |vb|
  #   # Display the VirtualBox GUI when booting the machine
  #   vb.gui = true
  #
  #   # Customize the amount of memory on the VM:
  #   vb.memory = "1024"
  # end
  #
  # View the documentation for the provider you are using for more
  # information on available options.
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
  end

  # default synced folder
  config.vm.synced_folder ".", "/vagrant"
  config.vm.synced_folder "~/.m2", "/home/vagrant/.m2",
                          owner: "vagrant", group: "vagrant", mount_options: ["uid=900", "gid=900"]

  # Enable provisioning with a shell script. Additional provisioners such as
  # Puppet, Chef, Ansible, Salt, and Docker are also available. Please see the
  # documentation for more information about their specific syntax and use.
  # config.vm.provision "shell", inline: <<-SHELL
  #   apt-get update
  #   apt-get install -y apache2
  # SHELL

  config.vm.provision "file", source: "src/test/ssh-keys/id_rsa", destination: ".ssh/id_rsa"
  config.vm.provision "file", source: "src/test/ssh-keys/id_rsa.pub", destination: ".ssh/id_rsa.pub"
  config.vm.provision "file", source: "src/test/ssh-keys/id_rsa-passphrase", destination: ".ssh/id_rsa-passphrase"
  config.vm.provision "file", source: "src/test/ssh-keys/id_rsa-passphrase.pub", destination: ".ssh/id_rsa-passphrase.pub"

  config.vm.provision "file", source: "vagrant/docker_ubuntu_gpg", destination: "docker_ubuntu_gpg"
  config.vm.provision "shell", inline: <<-SHELL
    echo working directory $(pwd)
    ls -la

    
    export ARIA2C_DOWNLOAD="aria2c --file-allocation=none -c -x 10 -s 10 -m 0 --console-log-level=notice --log-level=notice --summary-interval=0"


    chown vagrant:vagrant /home/vagrant/.ssh/id_rsa*
    chmod 400 /home/vagrant/.ssh/id_rsa /home/vagrant/.ssh/id_rsa-passphrase
    chmod 644 /home/vagrant/.ssh/id_rsa.pub /home/vagrant/.ssh/id_rsa-passphrase.pub


    mkdir -p /var/run/sshd
    sed -Ei 's/^[#]?Port[ ]+22$/Port 8022/' /etc/ssh/sshd_config
    sed -Ei 's/^[#]?UseDNS yes/UseDNS no/' /etc/ssh/sshd_config
    sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config
    sed -i 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' /etc/pam.d/sshd

    RSYNC_USERNAME=${RSYNC_USERNAME:-vagrant}
    RSYNC_PASSWORD=${RSYNC_PASSWORD:-comeonFrance!:-)}
    echo -e "\nexport RSYNC_PASSWORD='comeonFrance!:-)'" >> /home/vagrant/.profile
    RSYNC_HOSTS_ALLOW=${RSYNC_HOSTS_ALLOW:-192.168.33.0/24 172.17.0.0/16 192.168.0.0/16 127.0.0.1/32 ::1}
    RSYNC_VOLUME_NAME=${RSYNC_VOLUME_NAME:-volume}
    RSYNC_VOLUME_PATH=${RSYNC_VOLUME_PATH:-/volume}

    if [[ -e "/home/vagrant/.ssh/authorized_keys" ]]; then
        chown vagrant:vagrant /home/vagrant/.ssh/authorized_keys
        chmod 644 /home/vagrant/.ssh/authorized_keys
        echo -e '\n' >> /home/vagrant/.ssh/authorized_keys
        cat /home/vagrant/.ssh/id_rsa.pub >> /home/vagrant/.ssh/authorized_keys
    else
        cp /home/vagrant/.ssh/id_rsa.pub /home/vagrant/.ssh/authorized_keys
    fi
    echo -e '\n' >> /home/vagrant/.ssh/authorized_keys
    cat /home/vagrant/.ssh/id_rsa-passphrase.pub >> /home/vagrant/.ssh/authorized_keys
    echo "vagrant:${RSYNC_PASSWORD}" | chpasswd

    echo "${RSYNC_USERNAME}:${RSYNC_PASSWORD}" > /etc/rsyncd.secrets
    chown vagrant:root /etc/rsyncd.secrets
    chmod 0440 /etc/rsyncd.secrets
    mkdir -p ${RSYNC_VOLUME_PATH}
    chown vagrant:root ${RSYNC_VOLUME_PATH}
    chmod g+rxw,u+rxw ${RSYNC_VOLUME_PATH}

    [[ -f /etc/rsyncd.conf ]] || cat <<EOF > /etc/rsyncd.conf
auth users = ${RSYNC_USERNAME}
#gid = vagrant
hosts allow = ${RSYNC_HOSTS_ALLOW}
hosts deny = *
list = yes
lock file = /var/lock/rsyncd.lock
#log file = /dev/stdout
log file = /var/log/rsync.log
max connections = 10
#pid file = /var/run/rsyncd.pid
port = 873
read only = false
secrets file = /etc/rsyncd.secrets
timeout = 300
#uid = vagrant
use chroot = no
[${RSYNC_VOLUME_NAME}]
    comment = ${RSYNC_VOLUME_PATH} directory
    path = ${RSYNC_VOLUME_PATH}
EOF

    sed -Ei 's/[#]?RSYNC_ENABLE=false$/RSYNC_ENABLE=inetd/' /etc/default/rsync
    mkdir -p /etc/xinetd.d
    [[ -f /etc/xinetd.d/rsync ]] || cat <<EOF > /etc/xinetd.d/rsync
service rsync
{
    disable         = no
    flags	        	= IPv4
    socket_type     = stream
    wait            = no
    user            = vagrant
    server          = /usr/bin/rsync
    server_args     = --daemon --config=/etc/rsyncd.conf
    log_on_failure  += USERID
}
EOF
    /etc/init.d/xinetd restart
    systemctl status xinetd
    #/etc/init.d/rsync restart
    netstat -anop | grep 873


    #git clone -b master https://github.com/ci-and-cd/docker-rsync.git /home/vagrant/docker-rsync
    echo -e '\nexport EXTERNAL_RSYNC_873_PORT=8873' >> /home/vagrant/.profile
    echo -e '\nexport EXTERNAL_SSH_22_PORT=8022' >> /home/vagrant/.profile

    echo -e '\nLANG=en_US.utf-8' >> /etc/environment
    echo -e '\nLC_ALL=en_US.utf-8' >> /etc/environment

    echo -e '\nexport CI_OPT_GPG_KEYNAME=59DBF10E' >> /home/vagrant/.profile

    chown -R vagrant:vagrant /home/vagrant
  SHELL
end
