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
import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;
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
public class RsyncSshExternalWagon extends AbstractSshWagon implements RsyncWagon {

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

    public RsyncSshExternalWagon() {
        this.interactive = false;
    }

    @Override
    public String getExecutable() {
        return this.getRsyncExecutable();
    }

    @Override
    public void putDirectory(
        final File sourceDirectory,
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        RsyncWagon.super.putDirectory(sourceDirectory, destinationDirectory);
    }

    @Override
    public void putMkdirRemote(
        final String destination
    ) throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        RsyncWagon.super.putMkdirRemote(destination);
    }

    @Override
    public void putSetPermissionRemote(final Resource resource, final String destination) throws TransferFailedException {
        super.putSetPermissionRemote(resource, destination);
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
    public Commandline createBaseCommandLine(
        final File privateKey,
        final String... options
    ) {
        final Commandline cl = RsyncWagon.super.createBaseCommandLine(options);
        cl.createArg().setValue("-e"); // specify the remote shell to use
        cl.createArg().setValue(this.createSshCommandLine(privateKey));
        return cl;
    }

    @Override
    public boolean isInteractive() {
        return false; // unsupported
    }

    @Override
    public void setInteractive(final boolean interactive) {
        // unsupported
    }

    private String createSshCommandLine(final File privateKey) {
        final StringBuilder ssh = new StringBuilder(this.getSshExecutable()).append(" ");
        if (privateKey != null) {
            ssh.append("-i ").append(privateKey.getAbsolutePath()).append(" ");
        }
        if (this.getSshArgs() != null) {
            ssh.append(this.getSshArgs()).append(" ");
        }

        if (this.getSshArgs() == null || !this.getSshArgs().contains(" StrictHostKeyChecking=")) {
            ssh.append("-o StrictHostKeyChecking=no ");
        }

        if (this.getSshArgs() == null || !this.getSshArgs().contains(" UserKnownHostsFile=")) {
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

        return ssh.toString().trim();
    }
}
