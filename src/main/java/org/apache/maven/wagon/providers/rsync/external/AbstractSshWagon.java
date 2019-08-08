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

import java.util.List;

import lombok.Getter;

import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;

public abstract class AbstractSshWagon extends AbstractExternalWagon implements SshCommandExecutor {

    @Getter
    private ScpHelper sshTool = new ScpHelper(this);

    @Override
    public List<String> getFileList(
        final String destinationDirectory
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return this.getSshTool().getFileList(destinationDirectory, this.getRepository());
    }

    @Override
    public boolean resourceExists(
        final String resourceName
    ) throws TransferFailedException, AuthorizationException {
        return this.getSshTool().resourceExists(resourceName, this.getRepository());
    }

    @Override
    protected void putMkdirRemote(
        final String destination
    ) throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException {
        Resource resource = new Resource(destination);

        String basedir = getRepository().getBasedir();

        String resourceName = AbstractExternalWagon.normalize(destination);

        String dir = PathUtils.dirname(resourceName);

        dir = AbstractExternalWagon.normalize(dir);

        String umaskCmd = null;
        if (getRepository().getPermissions() != null) {
            String dirPerms = getRepository().getPermissions().getDirectoryMode();

            if (dirPerms != null) {
                umaskCmd = "umask " + PermissionModeUtils.getUserMaskFor(dirPerms);
            }
        }

        String mkdirCmd = "mkdir -p " + basedir + "/" + dir + "\n";

        if (umaskCmd != null) {
            mkdirCmd = umaskCmd + "; " + mkdirCmd;
        }

        try {
            executeCommand(mkdirCmd);
        } catch (CommandExecutionException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

            throw new TransferFailedException("Error executing command for transfer", e);
        }
    }

    @Override
    protected void putSetPermissionRemote(final Resource resource, final String destination) throws TransferFailedException {
        String basedir = getRepository().getBasedir();

        String resourceName = AbstractExternalWagon.normalize(destination);

        try {
            RepositoryPermissions permissions = getRepository().getPermissions();

            if (permissions != null && permissions.getGroup() != null) {
                executeCommand("chgrp -f " + permissions.getGroup() + " " + basedir + "/" + resourceName + "\n",
                    true);
            }

            if (permissions != null && permissions.getFileMode() != null) {
                executeCommand("chmod -f " + permissions.getFileMode() + " " + basedir + "/" + resourceName + "\n",
                    true);
            }
        } catch (CommandExecutionException e) {
            fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

            throw new TransferFailedException("Error executing command for transfer", e);
        }
    }
}
