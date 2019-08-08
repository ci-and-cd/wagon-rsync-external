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
import java.nio.file.Path;

import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

public interface RsyncWagon extends ExternalWagon {

    default Commandline createBaseCommandLine(
        final String... options
    ) {
        final Commandline cl = new Commandline();
        cl.setExecutable(this.getExecutable());

        if (this.getRsyncArgs() != null) {
            cl.createArg().setLine(this.getRsyncArgs());
        }

        for (final String option : options) {
            cl.createArg().setValue(option);
        }
        return cl;
    }

    String getRsyncArgs();

    String getRsyncExecutable();

    @Override
    default void putDirectory(
        final File sourceDirectory,
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        this.createRemoteDirectory(destinationDirectory);

        final String destDir = AbstractExternalWagon.normalize(destinationDirectory);
        try {
            executeCopyCommand(
                new Resource(destDir),
                new File(sourceDirectory.getCanonicalPath()),
                true,
                "-a", // archive mode; equals -rlptgoD (no -H,-A,-X)
                "-c", // skip based on checksum, not mod-time & size
                "-v" // increase verbosity
            );
        } catch (final IOException ex) {
            throw new ResourceDoesNotExistException(ex.getMessage(), ex);
        }
    }

    default void putMkdirRemote(
        final String destination
    ) throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        this.createRemoteDirectory(PathUtils.dirname(destination));
    }

    default void putSetPermissionRemote(final Resource resource, final String destination) throws TransferFailedException {
        // no-op
    }

    default void createRemoteDirectory(
        final String destinationDirectory
    ) throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        // TODO use unique tmp dirs
        final String repoPrefix = "target/rsync/repo";
        final String repoDir = StringUtils.replace(this.getRepository().getBasedir(), " ", "\\ ");
        final File repoTemplate = new File(repoPrefix + AbstractExternalWagon.prefixSlash(repoDir));

        final File localFile;
        final String remoteFile;
        if (this.getRepository().getProtocol().contains("ssh")) {
            localFile = new File(repoPrefix);
            remoteFile = "/";
        } else { // module (1st level of path) must present when using "rsync://".
            final Path path = new File(repoDir).toPath();
            localFile = path.getNameCount() > 0 ? new File(repoPrefix + "/" + path.getName(0).toString()) : new File(repoPrefix);
            remoteFile = path.getNameCount() > 0 ? "/" + path.getName(0).toString() : repoDir;
        }
        repoTemplate.mkdirs();
        executeCopyCommand(
            new Resource("/"),
            remoteFile,
            localFile,
            true,
            "--recursive", // recurse into directories
            "--dirs" // transfer directories without recursing
        );

        final String destPrefix = "target/rsync/dest";
        final String destDir = AbstractExternalWagon.normalize(destinationDirectory);
        final File destTemplate = new File(destPrefix + AbstractExternalWagon.prefixSlash(destDir));
        destTemplate.mkdirs();
        executeCopyCommand(
            new Resource("/"),
            new File(destPrefix),
            true,
            "--recursive", // recurse into directories
            "--dirs" // transfer directories without recursing
        );
    }
}
