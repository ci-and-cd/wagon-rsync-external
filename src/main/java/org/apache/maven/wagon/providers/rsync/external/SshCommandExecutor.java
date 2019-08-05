package org.apache.maven.wagon.providers.rsync.external;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public interface SshCommandExecutor extends CommandExecutor, WagonHasAuthenticationInfo {

    int SSH_FATAL_EXIT_CODE = 255;

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
        Commandline cl = createSshBaseCommandLine(this.getSshExecutable(), privateKey);

        if (this.getSshArgs() != null) {
            cl.createArg().setLine(this.getSshArgs());
        }

        final String remoteHost = this.buildRemoteHost();

        cl.createArg().setValue(remoteHost);

        int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? ScpHelper.DEFAULT_SSH_PORT
            : this.getRepository().getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            cl.createArg().setLine("-p " + port);
        }

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

    default Commandline createSshBaseCommandLine(final String executable, final File privateKey) {
        final Commandline cl = new Commandline();

        cl.setExecutable(executable);

        if (privateKey != null) {
            cl.createArg().setValue("-i");
            cl.createArg().setFile(privateKey);
        }

        // should check interactive flag, but rsyncsshexe never works in interactive mode right now due to i/o streams
        cl.createArg().setValue("-o");
        cl.createArg().setValue("BatchMode yes");

        if (this.getSshArgs() == null || !this.getSshArgs().contains(" StrictHostKeyChecking=")) {
            cl.createArg().setValue("-o");
            cl.createArg().setValue("StrictHostKeyChecking=no");
        }

        if (this.getSshArgs() == null || !this.getSshArgs().contains(" UserKnownHostsFile=")) {
            final File knowHostsFile = new File("target/dummy_knowhost");
            if (knowHostsFile.exists()) {
                knowHostsFile.delete();
            }
            try {
                cl.createArg().setValue("-o");
                cl.createArg().setValue("UserKnownHostsFile=" + knowHostsFile.getCanonicalPath());
            } catch (final IOException ignored) {
                // ignored
            }
        }
        return cl;
    }
}
