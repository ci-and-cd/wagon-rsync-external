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

import lombok.Getter;
import lombok.Setter;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
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
public class ScpExternalWagon extends AbstractSshWagon {
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
    public String getExecutable() {
        return this.getScpExecutable();
    }

    @Override
    public void putDirectory(
        final File sourceDirectory,
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        this.getSshTool().putDirectory(this, sourceDirectory, destinationDirectory);
    }

    @Override
    protected Commandline createBaseCommandLine(
        final File privateKey,
        final String... options
    ) {
        final Commandline cl = createSshBaseCommandLine(this.getExecutable(), privateKey);

        int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? ScpHelper.DEFAULT_SSH_PORT
            : this.getRepository().getPort();
        if (port != ScpHelper.DEFAULT_SSH_PORT) {
            cl.createArg().setLine("-P " + port);
        }

        if (this.getScpArgs() != null) {
            cl.createArg().setLine(this.getScpArgs());
        }

        return cl;
    }
}
