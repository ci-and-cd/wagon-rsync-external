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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.WagonConstants;
import org.apache.maven.wagon.authorization.AuthorizationException;
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
 * role-hint=" rsyncexe"
 * instantiation-strategy="per-lookup"
 */
public class RsyncExternalWagon extends AbstractExternalWagon implements RsyncWagon {

    /**
     * Arguments to pass to the RSYNC command.
     *
     * @component.configuration
     */
    @Getter
    @Setter
    private String rsyncArgs = "--progress --compress --compress-level=6";
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
    public String buildRemoteHost() {
        final int port = this.getRepository().getPort() == WagonConstants.UNKNOWN_PORT
            ? 873 // DEFAULT_RSYNCD_PORT
            : this.getRepository().getPort();
        return "rsync://" + super.buildRemoteHost() + ":" + port;
    }

    @Override
    public Commandline createBaseCommandLine(
        final File privateKey,
        final String... options
    ) {
        final Commandline cl = RsyncWagon.super.createBaseCommandLine(options);
        if (options.length == 0) {
            cl.createArg().setValue("-a"); // archive mode; equals -rlptgoD (no -H,-A,-X)
            cl.createArg().setValue("--no-R");
            cl.createArg().setValue("--no-implied-dirs"); // don't send implied dirs with --relative
        }
        return cl;
    }

    @Override
    public String getExecutable() {
        return this.getRsyncExecutable();
    }

    @Override
    public List<String> getFileList(
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException {
        return this.getFileList(destinationDirectory, false);
    }

    @Override
    public boolean isInteractive() {
        return false; // unsupported
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
        RsyncWagon.super.putSetPermissionRemote(resource, destination);
    }

    @Override
    public boolean resourceExists(
        final String resourceName
    ) throws TransferFailedException {
        try {
            final List<String> fileList = this.getFileList(PathUtils.dirname(resourceName), false);
            return fileList.contains(new File(resourceName).getName());
        } catch (final ResourceDoesNotExistException ex) {
            return false;
        }
    }

    @Override
    public void setInteractive(final boolean interactive) {
        // unsupported
    }

    private Streams executeCommand(final Commandline cl, final boolean ignoreFailures) throws CommandExecutionException {
        fireSessionDebug("Executing command: " + cl.toString());

        if (this.getAuthenticationInfo().getPassword() != null) {
            cl.addEnvironment(RSYNC_PASSWORD, this.getAuthenticationInfo().getPassword());
        }

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
                if (!ignoreFailures) {
                    throw new CommandExecutionException("Exit code " + exitCode + " - " + err.getOutput());
                }
            }
            return streams;
        } catch (CommandLineException e) {
            throw new CommandExecutionException("Error executing command line", e);
        }
    }

    private List<String> getFileList(
        final String destinationDirectory,
        final boolean recursive
    ) throws TransferFailedException, ResourceDoesNotExistException {
        try {
            final Commandline cl = RsyncWagon.super.createBaseCommandLine();
            if (recursive) {
                cl.createArg().setValue("--recursive");
            }
            final String path = getPath(this.getRepository().getBasedir(), destinationDirectory);
            cl.createArg().setValue(this.buildRemoteHost() + StringUtils.replace(suffixSlash(path), " ", "\\ "));

            Streams streams = this.executeCommand(cl, false);
            return new RsyncListParser().parseFiles(streams.getOut()).stream()
                .filter(it -> !it.equals("./") && !it.equals("../"))
                .collect(toList());
        } catch (final CommandExecutionException ex) {
            if (ex.getMessage().trim().endsWith("No such file or directory (2)") || ex.getMessage().contains("Exit code 23 ")) {
                throw new ResourceDoesNotExistException(ex.getMessage().trim(), ex);
            } else {
                throw new TransferFailedException("Error performing file listing.", ex);
            }
        }
    }
}
