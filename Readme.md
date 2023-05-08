# POC for Apache MINA SSHD issue "ssh-rsa"

| Class              | Method         | Image         | Notes         |
|--------------------|----------------|---------------|---------------|
| PocOl8PasswordTest | run (success)  | oraclelinux:8 | Password only |
| PocOl8PubkeyTest   | run (success)  | oraclelinux:8 | Pubkey only   |
| PocOl9PasswordTest | run (success)  | oraclelinux:9 | Password only |
| PocOl9PubkeyTest   | run (failure)  | oraclelinux:9 | Pubkey only   |
| PocOl9PubkeyTest   | run2 (success) | oraclelinux:9 | Pubkey only   |

The main difference between the both OL9 pubkey tests is the workaround 
flag which basically flips the order of items for 
`client.setSignatureFactories()`.  The technical order when using 
`BuiltinSignatures.VALUES` is `dsa`, `dsa_cert`, `rsa`, `rsa_cert`, 
`rsaSHA256`, etc. When using an RSA key, the type is `ssh-rsa` 
and this is *always* `rsa` and this is always `RSA with SHA1`. That is
complicated, because SHA1 is being disabled on modern systems. General 
usage of RSA is fine, but not something combined with SHA1.

Meanwhile, OPENSSH may treat `ssh-rsa` as RSA with SHA256, but the MINA
SSHD client does not support this.

As a workaround, the list of supported signature algorithms can be tweaked
(because the order of appearance is used for matching). If the match finder
is trying `rsaSHA256` for `ssh-rsa` at first, it will never try the 
non-working `rsa` again.
