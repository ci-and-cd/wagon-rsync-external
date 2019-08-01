package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class TestData {

    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir", "target");
    }

    public static String getTestRepositoryUrl(int port) {
        return "rsyncexe://" + getHostname() + ":" + port + getRepoPath();
    }

    public static String getTestRepositoryUrl() {
        return "rsyncexe://" + getHostname() + getRepoPath();
    }

    public static String getRepoPath() {
        return getTempDirectory() + "/wagon-ssh-test/" + getUserName();
    }

    public static String getUserName() {
        return System.getProperty("test.user", System.getProperty("user.name"));
    }

    public static String getUserPassword() {
        return "comeonFrance!:-)";
    }

    public static File getPrivateKey() {
        return new File(System.getProperty("sshKeysPath", "src/test/ssh-keys"), "id_rsa");
    }

    public static String getHostname() {
        return System.getProperty("test.host", "localhost");
    }

    public static String getHostKey() {
        try {
            return FileUtils.fileRead(
                new File(System.getProperty("sshKeysPath"), "id_rsa.pub").getPath())
                .substring("ssh-rsa".length()).trim();
        } catch (IOException e) {
            return null;
        }
    }
}
