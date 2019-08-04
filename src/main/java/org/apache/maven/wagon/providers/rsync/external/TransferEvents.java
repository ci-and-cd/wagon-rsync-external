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

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferEventSupport;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.resource.Resource;

public interface TransferEvents extends Wagon {

    @Override
    default void addTransferListener(TransferListener listener) {
        this.getTransferEventSupport().addTransferListener(listener);
    }

    @Override
    default boolean hasTransferListener(TransferListener listener) {
        return this.getTransferEventSupport().hasTransferListener(listener);
    }

    @Override
    default void removeTransferListener(TransferListener listener) {
        this.getTransferEventSupport().removeTransferListener(listener);
    }

    TransferEventSupport getTransferEventSupport();

    default void fireTransferProgress(TransferEvent transferEvent, byte[] buffer, int n) {
        this.getTransferEventSupport().fireTransferProgress(transferEvent, buffer, n);
    }

    default void fireGetCompleted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferCompleted(transferEvent);
    }

    default void fireGetStarted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferStarted(transferEvent);
    }

    default void fireGetInitiated(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferInitiated(transferEvent);
    }

    default void firePutInitiated(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferInitiated(transferEvent);
    }

    default void firePutCompleted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferCompleted(transferEvent);
    }

    default void firePutStarted(Resource resource, File localFile) {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent(this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT);

        transferEvent.setTimestamp(timestamp);

        transferEvent.setLocalFile(localFile);

        this.getTransferEventSupport().fireTransferStarted(transferEvent);
    }

    default void fireTransferDebug(String message) {
        this.getTransferEventSupport().fireDebug(message);
    }
}
