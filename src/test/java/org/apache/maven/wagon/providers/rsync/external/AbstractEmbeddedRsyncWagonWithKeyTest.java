package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.wagon.StreamingWagonTestCase;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.SshServerEmbedded;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractEmbeddedRsyncWagonWithKeyTest extends StreamingWagonTestCase {

    SshServerEmbedded sshServerEmbedded;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        String sshKeyResource = "ssh-keys/id_rsa.pub";

        sshServerEmbedded = new SshServerEmbedded(getProtocol(), Arrays.asList(sshKeyResource), true);

        sshServerEmbedded.start();
        System.out.println("sshd on port " + sshServerEmbedded.getPort());
    }

    @Override
    protected void tearDownWagonTestingFixtures() throws Exception {
        sshServerEmbedded.stop();
    }

    @Override
    protected abstract String getProtocol();

    protected int getTestRepositoryPort() {
        return sshServerEmbedded.getPort();
    }

    @Override
    public String getTestRepositoryUrl() {
        return TestData.getTestRepositoryUrl(sshServerEmbedded.getPort());
    }

    @Override
    protected AuthenticationInfo getAuthInfo() {
        AuthenticationInfo authInfo = super.getAuthInfo();
        // user : guest/guest123 -  passphrase : toto01
        authInfo.setUserName("guest");
        //authInfo.setPassword( TestData.getUserPassword() );
        authInfo.setPrivateKey(new File("src/test/ssh-keys/id_rsa").getPath());

        return authInfo;
    }

    @Override
    protected long getExpectedLastModifiedOnGet(Repository repository, Resource resource) {
        return new File(repository.getBasedir(), resource.getName()).lastModified();
    }

    public void testConnect() throws Exception {
        getWagon().connect(new Repository("foo", getTestRepositoryUrl()), getAuthInfo());
        assertTrue(true);
    }


    @Override
    protected boolean supportsGetIfNewer() {
        return false;
    }

    public void testWithSpaces() throws Exception {
        String dir = "foo   test";
        File spaceDirectory = new File(TestData.getRepoPath(), dir);
        if (spaceDirectory.exists()) {
            FileUtils.deleteDirectory(spaceDirectory);
        }
        spaceDirectory.mkdirs();

        String subDir = "foo bar";
        File sub = new File(spaceDirectory, subDir);
        if (sub.exists()) {
            FileUtils.deleteDirectory(sub);
        }
        sub.mkdirs();

        File dummy = new File("src/test/resources/dummy.txt");
        FileUtils.copyFileToDirectory(dummy, sub);

        String url = getTestRepositoryUrl() + "/" + dir;
        Repository repo = new Repository("foo", url);
        Wagon wagon = getWagon();
        wagon.connect(repo, getAuthInfo());
        List<String> files = wagon.getFileList(subDir);
        assertNotNull(files);
        assertEquals(1, files.size());
        assertTrue(files.contains("dummy.txt"));

        wagon.put(new File("src/test/resources/dummy.txt"), subDir + "/newdummy.txt");

        files = wagon.getFileList(subDir);
        assertNotNull(files);
        assertEquals(2, files.size());
        assertTrue(files.contains("dummy.txt"));
        assertTrue(files.contains("newdummy.txt"));

        File sourceWithSpace = new File("target/directory with spaces");
        if (sourceWithSpace.exists()) {
            FileUtils.deleteDirectory(sourceWithSpace);
        }
        File resources = new File("src/test/resources");

        FileUtils.copyDirectory(resources, sourceWithSpace);

        wagon.putDirectory(sourceWithSpace, "target with spaces");

        files = wagon.getFileList("target with spaces");

        assertNotNull(files);
        assertTrue(files.contains("dummy.txt"));
        assertFalse(files.contains("newdummy.txt"));
        assertTrue(files.contains("log4j.xml"));
    }
}
