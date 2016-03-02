/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.transaction.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.transaction.util.CommonsLoggingLogger;
import org.apache.commons.transaction.util.FileHelper;
import org.apache.commons.transaction.util.LoggerFacade;

/**
 * Tests for FileResourceManager.
 * 
 * @version $Id: FileResourceManagerTest.java 493628 2007-01-07 01:42:48Z joerg $
 */
public class FileResourceManagerVirtualAdminCommandsTest extends TestCase {

    private static final Log log = LogFactory
            .getLog(FileResourceManagerVirtualAdminCommandsTest.class.getName());

    private static final LoggerFacade sLogger = new CommonsLoggingLogger(log);

    private static final String ADMIN_COMMAND_PREFIX = "/extremelyUnlikelyPrefixForAdminCommands";

    private static final String STORE = "tmp/store";

    private static final String WORK = "tmp/work";

    private static final String ENCODING = "ISO-8859-15";

    private static final String[] INITIAL_FILES = new String[] { STORE + "/olli/Hubert6",
            STORE + "/olli/Hubert" };

    protected static final long TIMEOUT = Long.MAX_VALUE;

    private static void removeRec(String dirPath) {
        FileHelper.removeRec(new File(dirPath));
    }

    private static final void createFiles(String[] filePaths) {
        createFiles(filePaths, null, null);
    }

    private static final void createFiles(String[] filePaths, String[] contents, String dirPath) {
        for (int i = 0; i < filePaths.length; i++) {
            String filePath = filePaths[i];
            File file;
            if (dirPath != null) {
                file = new File(new File(dirPath), filePath);
            } else {
                file = new File(filePath);
            }
            file.getParentFile().mkdirs();
            try {
                file.delete();
                file.createNewFile();
                String content = null;
                if (contents != null && contents.length > i) {
                    content = contents[i];
                }
                if (content != null) {
                    FileOutputStream stream = new FileOutputStream(file);
                    stream.write(contents[i].getBytes(ENCODING));
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private static void reset() {
        removeRec(STORE);
        removeRec(WORK);
        new File(STORE).mkdirs();
        new File(WORK).mkdirs();
    }

    private static void createInitialFiles() {
        createFiles(INITIAL_FILES);
    }

    public static VirtualAdminCommandsFileResourceManager createFRM() {
        return new VirtualAdminCommandsFileResourceManager(STORE, WORK, false, sLogger, true) {
            public void setDirty(Object txId, Throwable t) {
                dirty = true;
            }

        };
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(FileResourceManagerVirtualAdminCommandsTest.class);
        return suite;
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public FileResourceManagerVirtualAdminCommandsTest(String testName) {
        super(testName);
    }

    public void testRecover() throws Throwable {
        sLogger.logInfo("Checking recover() admin command");
        reset();
        createInitialFiles();
        VirtualAdminCommandsFileResourceManager frm = createFRM();
        frm.setVirtualAdminPath(ADMIN_COMMAND_PREFIX);
        InputStream is = frm.readResource(ADMIN_COMMAND_PREFIX + "recover");
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        String line = bf.readLine();
        assertEquals(
                line,
                "Command recover failed with the following message: Recovery is possible in started or starting resource manager only: System error");
        frm.start();
        frm.setDirty(null, null);
        frm.startTransaction("newTx");
        boolean failed = false;
        try {
        frm.commitTransaction("newTx");
        } catch (ResourceManagerException rme) {
            failed = true;
        }
        assertTrue(failed);
        is = frm.readResource(ADMIN_COMMAND_PREFIX + "recover");
        bf = new BufferedReader(new InputStreamReader(is));
        line = bf.readLine();
        assertEquals(
                line,
                "Command recover terminated successfully");
        frm.startTransaction("newTx");
        failed = false;
        try {
            frm.commitTransaction("newTx");
            } catch (ResourceManagerException rme) {
                failed = true;
            }
            assertFalse(failed);
    }
}
