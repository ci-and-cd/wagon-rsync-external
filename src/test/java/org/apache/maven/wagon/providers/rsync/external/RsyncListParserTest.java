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

import junit.framework.TestCase;

import org.apache.maven.wagon.TransferFailedException;

public class RsyncListParserTest extends TestCase {

    public void testParseLinux()
        throws TransferFailedException {
        String rawLS = "Warning: Permanently added 'localhost' (ECDSA) to the list of known hosts.\n"
            + "receiving file list ... 3 files to consider\n"
            + "drwxr-xr-x            128 2019/08/04 00:10:20 .\n"
            + "-rw-r--r--             18 2019/08/04 00:10:20 test-resource\n"
            + "drwxr-xr-x            224 2019/08/04 00:10:20 file-list\n"
            + "drwxr-xr-x            256 2019/08/04 03:03:16 file-list";

        RsyncListParser parser = new RsyncListParser();
        List<String> files = parser.parseFiles(rawLS);
        assertNotNull(files);
        assertEquals(4, files.size());
        assertTrue(files.contains("test-resource"));
        assertTrue(files.contains("file-list/"));
    }
}
