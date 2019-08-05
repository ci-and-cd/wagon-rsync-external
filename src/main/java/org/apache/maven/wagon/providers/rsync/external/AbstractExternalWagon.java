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

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public abstract class AbstractExternalWagon extends AbstractWagon implements ExternalWagon {

    static final String RSYNC_PASSWORD = "RSYNC_PASSWORD"; // or use the '--password-file' option

    @Override
    protected void closeConnection() throws ConnectionException {
        // nothing to disconnect
    }

    protected abstract Commandline createBaseCommandLine(File privateKey, String... options);

    @Override
    public void executeCopyCommand(
        final Resource resource,
        final File localFile,
        final boolean put,
        final String... options
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final String resourceName = AbstractExternalWagon.normalizeResource(resource);
        final String remoteFile = getRepository().getBasedir() + prefixSlash(resourceName);

        this.executeCopyCommand(resource, remoteFile, localFile, put, options);
    }

    @Override
    public void executeCopyCommand(
        final Resource resource,
        final String remoteFile,
        final File localFile,
        final boolean put,
        final String... options
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        File privateKey;
        if (this.getRepository().getProtocol().contains("ssh")) {
            try {
                privateKey = ScpHelper.getPrivateKey(this.getAuthenticationInfo());
            } catch (final FileNotFoundException e) {
                fireSessionConnectionRefused();

                throw new AuthorizationException(e.getMessage());
            }
        } else {
            privateKey = null;
        }

        final Commandline cl = createBaseCommandLine(privateKey, options);

        cl.setWorkingDirectory(localFile.getParentFile().getAbsolutePath());

        if (!this.getRepository().getProtocol().contains("ssh") && this.getAuthenticationInfo().getPassword() != null) {
            cl.addEnvironment(RSYNC_PASSWORD, this.getAuthenticationInfo().getPassword());
        }

        final String remoteHost = this.buildRemoteHost();
        final String qualifiedRemoteFile = remoteHost
            + (remoteHost.contains(":") ? "" : ":")
            + StringUtils.replace(options.length > 0 ? suffixSlash(remoteFile) : remoteFile, " ", "\\ ");

        final String localFileName = options.length > 0 ? suffixSlash(localFile.getName()) : localFile.getName();

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
    public void fireTransferDebug(final String message) {
        super.fireTransferDebug(message);
    }

    @Override
    public void fireSessionDebug(final String message) {
        super.fireSessionDebug(message);
    }

    @Override
    public void get(
        final String resourceName,
        final File destination
    ) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        String path = AbstractExternalWagon.normalize(resourceName);

        Resource resource = new Resource(path);

        fireGetInitiated(resource, destination);

        createParentDirectories(destination);

        fireGetStarted(resource, destination);

        executeCopyCommand(resource, destination, false);

        postProcessListeners(resource, destination, TransferEvent.REQUEST_GET);

        fireGetCompleted(resource, destination);
    }

    @Override
    public boolean getIfNewer(final String resourceName, final File destination, long timestamp)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        fireSessionDebug("getIfNewer in SSH wagon is not supported - performing an unconditional get");
        get(resourceName, destination);
        return true;
    }

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        if (this.authenticationInfo == null) {
            this.authenticationInfo = new AuthenticationInfo();
        }
    }

    @Override
    public void put(final File source, final String destination)
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(destination);

        firePutInitiated(resource, source);

        if (!source.exists()) {
            throw new ResourceDoesNotExistException("Specified source file does not exist: " + source);
        }

        this.putMkdirRemote(destination);

        resource.setContentLength(source.length());

        resource.setLastModified(source.lastModified());

        firePutStarted(resource, source);

        executeCopyCommand(resource, source, true);

        postProcessListeners(resource, source, TransferEvent.REQUEST_PUT);

        this.putSetPermissionRemote(resource, destination);

        firePutCompleted(resource, source);
    }

    @Override
    public boolean supportsDirectoryCopy() {
        return ExternalWagon.super.supportsDirectoryCopy();
    }

    protected abstract void putMkdirRemote(
        String destination
    ) throws AuthorizationException, ResourceDoesNotExistException, TransferFailedException;

    protected abstract void putSetPermissionRemote(Resource resource, String destination) throws TransferFailedException;

    static String normalize(final String resourceName) {
        return StringUtils.replace(resourceName, "\\", "/");
    }

    static String normalizeResource(final Resource resource) {
        return AbstractExternalWagon.normalize(resource.getName());
    }

    static String prefixSlash(final String str) {
        return str == null || str.startsWith("/") ? str : "/" + str;
    }

    static String suffixSlash(final String str) {
        return str == null || str.endsWith("/") ? str : str + "/";
    }
}
