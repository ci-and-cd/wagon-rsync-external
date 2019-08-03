# wagon-rsync-external
Wagon provider that gets and puts artifacts with a preinstalled rsync client

```shell script
# embedded ssh server does not have rsync
#./mvnw -e -ntp -Dssh-tests -Dtest.user=root -Duser.name=root -Dssh-embedded=true clean verify;
```

Or
```shell script
vagrant up
vagrant ssh
cd /vagrant
./mvnw -Dssh-tests -Dtest.user=vagrant -Duser.name=vagrant clean verify
```

Or
```shell script
docker-compose -f docker-compose_rsync.yml up -d
./mvnw -Dssh-tests -Dtest.user=root -Duser.name=root clean verify
```

### Remote debug surefire tests

see: https://maven.apache.org/surefire/maven-surefire-plugin/examples/debugging.html

`./mvnw -e -ntp -Dtest.user=root -Duser.name=root -Dssh-tests -Dmaven.surefire.debug clean test;`
