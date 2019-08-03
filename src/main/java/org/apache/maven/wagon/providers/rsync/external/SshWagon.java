package org.apache.maven.wagon.providers.rsync.external;

import java.io.File;
import java.util.List;

import lombok.Getter;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.PermissionModeUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;

public abstract class SshWagon extends AbstractWagon implements SshCommandExecutor {

    @Getter
    private ScpHelper sshTool = new ScpHelper(this);

    @Override
    protected void closeConnection() throws ConnectionException {
        // nothing to disconnect
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        if (this.authenticationInfo == null) {
            this.authenticationInfo = new AuthenticationInfo();
        }
    }

    @Override
    public void get(final String resourceName, final File destination)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        String path = normalize(resourceName);

        Resource resource = new Resource(path);

        fireGetInitiated(resource, destination);

        createParentDirectories(destination);

        fireGetStarted(resource, destination);

        executeCopyCommand(resource, destination, false);

        postProcessListeners(resource, destination, TransferEvent.REQUEST_GET);

        fireGetCompleted(resource, destination);
    }

    @Override
    public List<String> getFileList(final String destinationDirectory)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return this.getSshTool().getFileList(destinationDirectory, this.getRepository());
    }

    @Override
    public boolean getIfNewer(final String resourceName, final File destination, long timestamp)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        fireSessionDebug("getIfNewer in SSH wagon is not supported - performing an unconditional get");
        get(resourceName, destination);
        return true;
    }

    @Override
    public void put(final File source, final String destination)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(destination);

        firePutInitiated(resource, source);

        if (!source.exists()) {
            throw new ResourceDoesNotExistException("Specified source file does not exist: " + source);
        }

        String basedir = getRepository().getBasedir();

        String resourceName = normalize(destination);

        String dir = PathUtils.dirname(resourceName);

        dir = normalize(dir);

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

        resource.setContentLength(source.length());

        resource.setLastModified(source.lastModified());

        firePutStarted(resource, source);

        executeCopyCommand(resource, source, true);

        postProcessListeners(resource, source, TransferEvent.REQUEST_PUT);

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
        firePutCompleted(resource, source);
    }

    @Override
    public boolean resourceExists(final String resourceName)
        throws TransferFailedException, AuthorizationException {
        return this.getSshTool().resourceExists(resourceName, this.getRepository());
    }

    @Override
    public boolean supportsDirectoryCopy() {
        return true;
    }

    @Override
    public void fireTransferDebug(final String message) {
        super.fireTransferDebug(message);
    }

    @Override
    public void fireSessionDebug(final String message) {
        super.fireSessionDebug(message);
    }

    protected String normalize(final String resourceName) {
        return StringUtils.replace(resourceName, "\\", "/");
    }

    protected String normalizeResource(final Resource resource) {
        return normalize(resource.getName());
    }

    protected abstract void executeCopyCommand(
        Resource resource,
        File localFile,
        boolean put,
        String... options
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException;
}
