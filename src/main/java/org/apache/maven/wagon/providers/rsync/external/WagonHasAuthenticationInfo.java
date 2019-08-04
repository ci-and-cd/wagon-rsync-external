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

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

public interface WagonHasAuthenticationInfo extends Wagon {

    /**
     * The hostname of the remote server prefixed with the username,
     * which comes either from the repository URL or from the authenticationInfo.
     *
     * @return remote host string
     */
    default String buildRemoteHost() {
        String username = this.getRepository().getUsername();
        if (username == null) {
            username = getAuthenticationInfo().getUserName();
        }

        if (username == null) {
            return getRepository().getHost();
        } else {
            return username + "@" + getRepository().getHost();
        }
    }

    AuthenticationInfo getAuthenticationInfo();
}
