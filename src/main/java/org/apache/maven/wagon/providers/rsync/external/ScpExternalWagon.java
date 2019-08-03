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
import java.util.Locale;

import lombok.Getter;
import lombok.Setter;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * SCP deployer using "external" scp program.  To allow for
 * ssh-agent type behavior, until we can construct a Java SSH Agent and interface for JSch.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo [BP] add compression flag
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 * role-hint="scpexe"
 * instantiation-strategy="per-lookup"
 */
public class ScpExternalWagon extends SshWagon {
    /**
     * Arguments to pass to the SCP command.
     *
     * @component.configuration
     */
    @Getter
    @Setter
    private String scpArgs;
    /**
     * The external SCP command to use - default is <code>scp</code>.
     *
     * @component.configuration default="scp"
     */
    @Getter
    @Setter
    private String scpExecutable = "scp";

    /**
     * Arguments to pass to the SSH command.
     *
     * @component.configuration
     */
    @Getter
    @Setter
    private String sshArgs;
    /**
     * The external SSH command to use - default is <code>ssh</code>.
     *
     * @component.configuration default="ssh"
     */
    @Getter
    @Setter
    private String sshExecutable = "ssh";

    @Override
    public void putDirectory(
        final File sourceDirectory,
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        this.getSshTool().putDirectory(this, sourceDirectory, destinationDirectory);
    }

    @Override
    protected void executeCopyCommand(
        final Resource resource,
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
        final Commandline cl = createSshScpBaseCommandLine(this.getScpExecutable(), privateKey);

        cl.setWorkingDirectory(localFile.getParentFile().getAbsolutePath());

        int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? ScpHelper.DEFAULT_SSH_PORT
            : this.getRepository().getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            cl.createArg().setLine("-P " + port);
        }

        if (this.getScpArgs() != null) {
            cl.createArg().setLine(this.getScpArgs());
        }

        String resourceName = normalizeResource(resource);
        String remoteFile = getRepository().getBasedir() + "/" + resourceName;

        remoteFile = StringUtils.replace(remoteFile, " ", "\\ ");

        String qualifiedRemoteFile = this.buildRemoteHost() + ":" + remoteFile;
        if (put) {
            cl.createArg().setValue(localFile.getName());
            cl.createArg().setValue(qualifiedRemoteFile);
        } else {
            cl.createArg().setValue(qualifiedRemoteFile);
            cl.createArg().setValue(localFile.getName());
        }

        fireSessionDebug("Executing command: " + cl.toString());

        try {
            CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            int exitCode = CommandLineUtils.executeCommandLine(cl, null, err);
            if (exitCode != 0) {
                if (!put
                    && err.getOutput().trim().toLowerCase(Locale.ENGLISH).contains("no such file or directory")) {
                    throw new ResourceDoesNotExistException(err.getOutput());
                } else {
                    TransferFailedException e =
                        new TransferFailedException("Exit code: " + exitCode + " - " + err.getOutput());

                    fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

                    throw e;
                }
            }
        } catch (CommandLineException e) {
            fireTransferError(resource, e, put ? TransferEvent.REQUEST_PUT : TransferEvent.REQUEST_GET);

            throw new TransferFailedException("Error executing command line", e);
        }
    }
}
