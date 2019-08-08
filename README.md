# wagon-rsync-external
Wagon provider that gets and puts artifacts with a preinstalled rsync executable


### Usage

Same as [wagon-ssh-external](https://maven.apache.org/wagon/wagon-providers/wagon-ssh-external/).

Both of rsync over ssh (repo urls start with rsyncsshexe://) and rsyncd:// (repo urls start with rsyncexe://) are supported.


Rsync performs no encryption on its own. 
If you don't use ssh, nor do you tunnel the rsync traffic through stunnel or some kind of VPN, then no encryption is performed.


If you use the rsync:// protocol scheme (i.e. when you connect to a rsyncd daemon) then password authentication is done using a 
MD4-based challenge-response system and is probably still reasonably secure.


You can use `-Dwagon.privateKeyDirectory=/path/to/keys` (in systemProperties) to tell 
`org.apache.maven.wagon.providers.ssh.ScpHelper#findPrivateKey` where to find private keys.


### How to run tests

```shell script
# embedded ssh server does not support rsync
#./mvnw -e -ntp -Dssh-tests -Dtest.user=root -Duser.name=root -Dssh-embedded=true clean verify;
```

Or
```shell script
vagrant up
vagrant ssh
cd /vagrant
./mvnw -Dssh-tests -Dtest.rsyncdPort=873 -Dtest.sshPort=22 -Dtest.user=vagrant -Duser.name=vagrant clean verify
# or debug
./mvnw -Dmaven.surefire.debug -Dssh-tests -Dtest.host=192.168.33.20 -Dtest.rsyncdPort=873 -Dtest.sshPort=22 -Dtest.user=vagrant -Duser.name=vagrant clean verify
# or debug
docker-compose -f docker-compose_rsync.yml up -d
./mvnw -Dmaven.surefire.debug -Dssh-tests -Dtest.rsyncdPort=8873 -Dtest.sshPort=8022 -Dtest.user=root -Duser.name=root clean verify
```

Or
```shell script
docker-compose -f docker-compose_rsync.yml up -d
./mvnw -Dssh-tests -Dtest.rsyncdPort=8873 -Dtest.sshPort=8022 -Dtest.user=root -Duser.name=root clean verify
```


### Remote debug surefire tests

see: https://maven.apache.org/surefire/maven-surefire-plugin/examples/debugging.html

`./mvnw -e -ntp -Dmaven.surefire.debug -Dssh-tests -Dtest.user=root -Duser.name=root clean test;`
