package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public interface SshCommandExecutor extends CommandExecutor {

    int SSH_FATAL_EXIT_CODE = 255;

    AuthenticationInfo getAuthenticationInfo();

    String getSshExecutable();

    String getSshArgs();

    void fireTransferDebug(String message);

    void fireSessionDebug(String message);

    @Override
    default void executeCommand(final String command) throws CommandExecutionException {
        this.fireTransferDebug("Executing command: " + command);

        executeCommand(command, false);
    }

    @Override
    default Streams executeCommand(final String command, final boolean ignoreFailures) throws CommandExecutionException {
        File privateKey;
        try {
            privateKey = ScpHelper.getPrivateKey(this.getAuthenticationInfo());
        } catch (FileNotFoundException e) {
            throw new CommandExecutionException(e.getMessage(), e);
        }
        Commandline cl = createSshScpBaseCommandLine(this.getSshExecutable(), privateKey);

        int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? ScpHelper.DEFAULT_SSH_PORT
            : this.getRepository().getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            cl.createArg().setLine("-p " + port);
        }

        if (this.getSshArgs() != null) {
            cl.createArg().setLine(this.getSshArgs());
        }

        final String remoteHost = this.buildRemoteHost();

        cl.createArg().setValue(remoteHost);

        cl.createArg().setValue(command);

        fireSessionDebug("Executing command: " + cl.toString());

        try {
            CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
            Streams streams = new Streams();
            streams.setOut(out.getOutput());
            streams.setErr(err.getOutput());
            fireSessionDebug(streams.getOut());
            fireSessionDebug(streams.getErr());
            if (exitCode != 0) {
                if (!ignoreFailures || exitCode == SSH_FATAL_EXIT_CODE) {
                    throw new CommandExecutionException("Exit code " + exitCode + " - " + err.getOutput());
                }
            }
            return streams;
        } catch (CommandLineException e) {
            throw new CommandExecutionException("Error executing command line", e);
        }
    }

    /**
     * The hostname of the remote server prefixed with the username,
     * which comes either from the repository URL or from the authenticationInfo.
     *
     * @return remote host string
     */
    default String buildRemoteHost() {
        String username = this.getRepository().getUsername();
        if (username == null) {
            username = getAuthenticationInfo().getUserName();
        }

        if (username == null) {
            return getRepository().getHost();
        } else {
            return username + "@" + getRepository().getHost();
        }
    }

    default Commandline createSshScpBaseCommandLine(final String executable, final File privateKey) {
        final Commandline cl = new Commandline();

        cl.setExecutable(executable);

        if (privateKey != null) {
            cl.createArg().setValue("-i");
            cl.createArg().setFile(privateKey);
        }

        // should check interactive flag, but rsyncsshexe never works in interactive mode right now due to i/o streams
        cl.createArg().setValue("-o");
        cl.createArg().setValue("BatchMode yes");
        return cl;
    }
}
