package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class TestData {

    private static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir", "/target");
    }

    public static String getTestRepositoryUrl(String protocol, int port) {
        return protocol + "://" + getHostname() + ":" + port + getRepoPath();
    }

    public static String getTestRepositoryUrl(String protocol) {
        return protocol + "://" + getHostname() + getRepoPath();
    }

    private static String getRepoPath() {
        return "/volume" + getTempDirectory() + "/wagon-ssh-test/" + getUserName();
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
