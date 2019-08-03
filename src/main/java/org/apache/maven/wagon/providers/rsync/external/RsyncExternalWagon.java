package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * RSYNC deployer using "external" rsync program.  To allow for
 * ssh-agent type behavior, until we can construct a Java SSH Agent and interface for JSch.
 *
 * @author <a href="mailto:chshawkn@users.noreply.github.com">Haolun Zhang</a>
 * @todo [BP] add compression flag
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="rsyncsshexe"
 * instantiation-strategy="per-lookup"
 */
public class RsyncExternalWagon extends SshWagon {

    /**
     * The external SSH command to use - default is <code>ssh</code>.
     *
     * @component.configuration default="ssh"
     */
    @Getter
    @Setter
    private String sshExecutable = "ssh";

    /**
     * Arguments to pass to the SSH command.
     *
     * @component.configuration
     */
    @Getter
    @Setter
    private String sshArgs;

    /**
     * Arguments to pass to the RSYNC command.
     *
     * @component.configuration
     */
    @Getter
    @Setter
    private String rsyncArgs = "--progress";
    /**
     * The external RSYNC command to use - default is <code>rsync</code>.
     *
     * @component.configuration default="rsync"
     */
    @Getter
    @Setter
    private String rsyncExecutable = "rsync";

    public RsyncExternalWagon() {
        this.interactive = false;
    }

    @Override
    public void putDirectory(
        final File sourceDirectory,
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        // TODO use unique tmp dirs
        final String repoPrefix = "target/rsync/repo";
        final String repoDir = StringUtils.replace(getRepository().getBasedir(), " ", "\\ ");
        final File repoTemplate = new File(repoPrefix + prefixSlash(repoDir));
        repoTemplate.mkdirs();
        executeCopyCommand(new Resource("/"), "/", new File(repoPrefix), true, "-rd");

        final String destPrefix = "target/rsync/dest";
        final String destDir = this.normalize(destinationDirectory);
        final File destTemplate = new File(destPrefix + prefixSlash(destDir));
        destTemplate.mkdirs();
        executeCopyCommand(new Resource("/"), new File(destPrefix), true, "-rd");

        try {
            executeCopyCommand(new Resource(destDir), new File(sourceDirectory.getCanonicalPath()), true, "-avc");
        } catch (final IOException ex) {
            throw new ResourceDoesNotExistException(ex.getMessage(), ex);
        }
    }

    private void createDirectory() {

    }

    @Override
    protected void executeCopyCommand(
        final Resource resource,
        final File localFile,
        final boolean put,
        final String... options
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final String resourceName = normalizeResource(resource);
        String remoteFile = getRepository().getBasedir() + prefixSlash(resourceName);
        if (options.length > 0 && !remoteFile.endsWith("/")) {
            remoteFile = remoteFile + "/";
        }

        this.executeCopyCommand(resource, remoteFile, localFile, put, options);
    }

    private void executeCopyCommand(
        final Resource resource,
        final String remoteFile,
        final File localFile,
        final boolean put,
        final String... options
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        File privateKey;
        try {
            privateKey = ScpHelper.getPrivateKey(this.getAuthenticationInfo());
        } catch (FileNotFoundException e) {
            fireSessionConnectionRefused();

            throw new AuthorizationException(e.getMessage());
        }
        final Commandline cl = createRsyncBaseCommandLine(this.getRsyncExecutable(), privateKey);

        cl.setWorkingDirectory(localFile.getParentFile().getAbsolutePath());

        if (options.length == 0) {
            cl.createArg().setValue("-az");
            cl.createArg().setValue("--no-R");
            cl.createArg().setValue("--no-implied-dirs");
        } else {
            for (final String option : options) {
                cl.createArg().setValue(option);
            }
        }

        final String qualifiedRemoteFile = this.buildRemoteHost() + ":" + StringUtils.replace(remoteFile, " ", "\\ ");

        String localFileName = localFile.getName();
        if (options.length > 0 && !localFileName.endsWith("/")) {
            localFileName = localFileName + "/";
        }

        // if (options.length > 0 && put) {
        //     cl.createArg().setValue("--rsync-path=\"mkdir -p " + remoteFile + " && rsync\"");
        // }

        if (put) {
            cl.createArg().setValue(localFileName);
            cl.createArg().setValue(qualifiedRemoteFile);
        } else {
            cl.createArg().setValue(qualifiedRemoteFile);
            cl.createArg().setValue(localFileName);
        }

        fireSessionDebug("Executing command: " + cl.toString());

        try {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            final int exitCode = CommandLineUtils.executeCommandLine(cl, null, err);
            if (exitCode != 0) {
                if (!put
                    && err.getOutput().trim().toLowerCase(Locale.ENGLISH).contains("no such file or directory")
                ) {
                    throw new ResourceDoesNotExistException(err.getOutput());
                } else {
                    final TransferFailedException e = new TransferFailedException("Exit code: " + exitCode + " - " + err.getOutput());

                    fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

                    throw e;
                }
            }
        } catch (final CommandLineException ex) {
            fireTransferError(resource, ex, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

            throw new TransferFailedException("Error executing command line", ex);
        }
    }

    @Override
    public void connect(
        final Repository repository,
        final AuthenticationInfo authenticationInfo,
        final ProxyInfoProvider proxyInfoProvider
    ) throws ConnectionException, AuthenticationException {
        super.connect(repository, authenticationInfo, proxyInfoProvider);
    }

    @Override
    public boolean isInteractive() {
        return false; // unsupported
    }

    @Override
    public void setInteractive(final boolean interactive) {
        // unsupported
    }

    private Commandline createRsyncBaseCommandLine(final String executable, final File privateKey) {
        final StringBuilder ssh = new StringBuilder(this.getSshExecutable()).append(" ");
        if (privateKey != null) {
            ssh.append("-i ").append(privateKey.getAbsolutePath()).append(" ");
        }
        if (this.getSshArgs() != null) {
            ssh.append(this.getSshArgs()).append(" ");
        }

        if (this.getSshArgs() != null && !this.getSshArgs().contains(" StrictHostKeyChecking=")) {
            ssh.append("-o StrictHostKeyChecking=no ");
        }

        if (this.getSshArgs() != null && !this.getSshArgs().contains(" UserKnownHostsFile=")) {
            final File knowHostsFile = new File("target/dummy_knowhost");
            if (knowHostsFile.exists()) {
                knowHostsFile.delete();
            }
            try {
                ssh.append("-o UserKnownHostsFile=").append(knowHostsFile.getCanonicalPath()).append(" ");
            } catch (final IOException ignored) {
                // ignored
            }
        }

        int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? ScpHelper.DEFAULT_SSH_PORT
            : this.getRepository().getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            ssh.append("-P ").append(port).append(" ");
        }

        final String username = this.getRepository().getUsername() != null
            ? this.getRepository().getUsername()
            : this.getAuthenticationInfo().getUserName();
        if (username != null) {
            ssh.append("-l ").append(username).append(" ");
        }


        final Commandline rsyncCl = new Commandline();
        rsyncCl.setExecutable(executable);

        if (this.getRsyncArgs() != null) {
            rsyncCl.createArg().setLine(this.getRsyncArgs());
        }

        rsyncCl.createArg().setValue("-e");
        rsyncCl.createArg().setValue(ssh.toString().trim());
        return rsyncCl;
    }

    private static String prefixSlash(final String str) {
        return str == null || str.startsWith("/") ? str : "/" + str;
    }
}
