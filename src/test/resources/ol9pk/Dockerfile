FROM oraclelinux:9

RUN (dnf update -y; \
    dnf install -y openssh-server openssh-clients passwd; \
    dnf clean all)

RUN (ssh-keygen -A; \
     sed -i 's/UsePAM yes/#UsePAM yes/g' /etc/ssh/sshd_config; \
     sed -i 's/#UsePAM no/UsePAM no/g' /etc/ssh/sshd_config; \
     sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config; \
     sed -i 's/#PubkeyAuthentication yes/PubkeyAuthentication yes/' /etc/ssh/sshd_config)

RUN (mkdir -p /root/.ssh/; \
     echo "StrictHostKeyChecking=no" > /root/.ssh/config; \
     echo "UserKnownHostsFile=/dev/null" >> /root/.ssh/config)

ADD id_rsa.pub /root/.ssh/authorized_keys

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
