package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.FileUtils;

public class TestData {

    private static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir", "/target");
    }

    private static String getTestRepositoryUrl(String protocol, int port) {
        return protocol + "://" + getHostname() + ":" + port + getRepoPath();
    }

    public static String getTestRepositoryUrl(String protocol) {
        final Integer port = getPort(protocol);
        return port == null
            ? protocol + "://" + getHostname() + getRepoPath()
            : getTestRepositoryUrl(protocol, port);
    }

    public static Integer getPort(String protocol) {
        final String port;
        if ("rsyncexe".equals(protocol)) {
            port = System.getProperty("test.rsyncdPort");
        } else {
            port = System.getProperty("test.sshPort");
        }
        return port == null ? null : Integer.parseInt(port);
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
