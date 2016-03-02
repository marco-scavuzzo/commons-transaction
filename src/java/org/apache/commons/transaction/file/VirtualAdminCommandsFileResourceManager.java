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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.transaction.util.LoggerFacade;

/**
 * A resource manager for streamable objects stored in a file system that
 * features additional administration commands.
 * 
 * @version $Id: FileResourceManager.java 519647 2007-03-18 17:50:02Z
 *          ozeigermann $
 */
public class VirtualAdminCommandsFileResourceManager extends
        FileResourceManager implements ResourceManager,
        ResourceManagerErrorCodes {

    protected String virtualAdminPath = null;

    public String getVirtualAdminPath() {
        return virtualAdminPath;
    }

    public void setVirtualAdminPath(String virutalAdminPath) {
        this.virtualAdminPath = virutalAdminPath;
    }

    /**
     * Creates a new resource manager operation on the specified directories.
     * 
     * @param storeDir
     *            directory where main data should go after commit
     * @param workDir
     *            directory where transactions store temporary data
     * @param urlEncodePath
     *            if set to <code>true</code> encodes all paths to allow for
     *            any kind of characters
     * @param logger
     *            the logger to be used by this store
     */
    public VirtualAdminCommandsFileResourceManager(String storeDir,
            String workDir, boolean urlEncodePath, LoggerFacade logger) {
        this(storeDir, workDir, urlEncodePath, logger, false);
    }

    /**
     * Creates a new resource manager operation on the specified directories.
     * 
     * @param storeDir
     *            directory where main data should go after commit
     * @param workDir
     *            directory where transactions store temporary data
     * @param urlEncodePath
     *            if set to <code>true</code> encodes all paths to allow for
     *            any kind of characters
     * @param logger
     *            the logger to be used by this store
     * @param debug
     *            if set to <code>true</code> logs all locking information to
     *            "transaction.log" for debugging inspection
     */
    public VirtualAdminCommandsFileResourceManager(String storeDir,
            String workDir, boolean urlEncodePath, LoggerFacade logger,
            boolean debug) {
        this(storeDir, workDir, urlEncodePath ? new URLEncodeIdMapper() : null,
                new NoOpTransactionIdToPathMapper(), logger, debug);
    }

    /**
     * Creates a new resource manager operation on the specified directories.
     * This constructor is reintroduced for backwards API compatibility and is
     * used by jakarta-slide.
     * 
     * @param storeDir
     *            directory where main data should go after commit
     * @param workDir
     *            directory where transactions store temporary data
     * @param idMapper
     *            mapper for resourceId to path
     * @param logger
     *            the logger to be used by this store
     * @param debug
     *            if set to <code>true</code> logs all locking information to
     *            "transaction.log" for debugging inspection
     */
    public VirtualAdminCommandsFileResourceManager(String storeDir,
            String workDir, ResourceIdToPathMapper idMapper,
            LoggerFacade logger, boolean debug) {
        this(storeDir, workDir, idMapper, new NoOpTransactionIdToPathMapper(),
                logger, debug);
    }

    /**
     * Creates a new resource manager operation on the specified directories.
     * 
     * @param storeDir
     *            directory where main data should go after commit
     * @param workDir
     *            directory where transactions store temporary data
     * @param idMapper
     *            mapper for resourceId to path
     * @param txIdMapper
     *            mapper for transaction id to path
     * @param logger
     *            the logger to be used by this store
     * @param debug
     *            if set to <code>true</code> logs all locking information to
     *            "transaction.log" for debugging inspection
     */
    public VirtualAdminCommandsFileResourceManager(String storeDir,
            String workDir, ResourceIdToPathMapper idMapper,
            TransactionIdToPathMapper txIdMapper, LoggerFacade logger,
            boolean debug) {
        super(workDir, storeDir, idMapper, txIdMapper, logger, debug);
    }

    public boolean resourceExists(Object resourceId)
            throws ResourceManagerException {
        if (isVirtualAdminId(resourceId)) {
            logger
                    .logFine("Faking existence of virtual administration command");
            return true;
        }

        return super.resourceExists(resourceId);
    }

    public boolean resourceExists(Object txId, Object resourceId)
            throws ResourceManagerException {
        if (isVirtualAdminId(resourceId)) {
            logger
                    .logFine("Faking existence of virtual administration command");
            return true;
        }

        return super.resourceExists(txId, resourceId);
    }

    public void deleteResource(Object txId, Object resourceId)
            throws ResourceManagerException {

        checkForVirtualAdminCommand(resourceId);

        super.deleteResource(txId, resourceId);
    }

    public void deleteResource(Object txId, Object resourceId,
            boolean assureOnly) throws ResourceManagerException {

        checkForVirtualAdminCommand(resourceId);

        super.deleteResource(txId, resourceId, assureOnly);
    }

    public void createResource(Object txId, Object resourceId)
            throws ResourceManagerException {

        checkForVirtualAdminCommand(resourceId);

        super.createResource(txId, resourceId);
    }

    public void createResource(Object txId, Object resourceId,
            boolean assureOnly) throws ResourceManagerException {

        checkForVirtualAdminCommand(resourceId);

        super.createResource(txId, resourceId, assureOnly);
    }

    public void copyResource(Object txId, Object fromResourceId,
            Object toResourceId, boolean overwrite)
            throws ResourceManagerException {

        checkForVirtualAdminCommand(fromResourceId);
        checkForVirtualAdminCommand(toResourceId);

        super.copyResource(txId, fromResourceId, toResourceId, overwrite);
    }

    public void moveResource(Object txId, Object fromResourceId,
            Object toResourceId, boolean overwrite)
            throws ResourceManagerException {

        checkForVirtualAdminCommand(fromResourceId);
        checkForVirtualAdminCommand(toResourceId);

        super.moveResource(txId, fromResourceId, toResourceId, overwrite);
    }

    public InputStream readResource(Object resourceId)
            throws ResourceManagerException {

        if (isVirtualAdminId(resourceId)) {
            logger.logWarning("Issuing virtual admin command" + resourceId);
            return executeAdminCommand(resourceId);
        }

        return super.readResource(resourceId);
    }

    public InputStream readResource(Object txId, Object resourceId)
            throws ResourceManagerException {

        if (isVirtualAdminId(resourceId)) {
            String message = "You must not call virtual admin commands ("
                    + resourceId + ") from within transactions!";
            logger.logSevere(message);
            throw new ResourceManagerException(message);
        }

        return super.readResource(txId, resourceId);
    }

    protected void checkForVirtualAdminCommand(Object resourceId)
            throws ResourceManagerException {
        if (isVirtualAdminId(resourceId)) {
            String message = "You must not make modification calls to virtual admin commands ("
                    + resourceId + ")!";
            logger.logSevere(message);
            throw new ResourceManagerException(message);
        }
    }

    protected boolean isVirtualAdminId(Object resourceId) {
        return (getVirtualAdminPath() != null && resourceId.toString()
                .startsWith(getVirtualAdminPath()));
    }

    protected InputStream executeAdminCommand(Object resourceId) {
        StringBuffer sb = new StringBuffer();

        if (!isVirtualAdminId(resourceId)) {
            String message = "Internal error: " + resourceId.toString()
                    + " is no administration command, but is supposed to!";
            sb.append(message);
            logger.logSevere(message);
        } else {
            String command = resourceId.toString().substring(
                    getVirtualAdminPath().length());
            logger.logInfo("Processing admin command " + command);

            // XXX this really should be more flexible
            try {
                if (isAKnowCommand(command)) {
                    if (command.equals("recover")) {
                        recover();
                    }

                    String message = "Command " + command
                            + " terminated successfully";
                    sb.append(message);
                    logger.logInfo(message);
                } else {
                    String message = "Command " + command + " unknown";
                    sb.append(message);
                    logger.logWarning(message);

                }
            } catch (ResourceManagerSystemException e) {
                String message = "Command " + command
                        + " failed with the following message: "
                        + e.getMessage();
                sb.append(message);
                logger.logSevere(message, e);
            }

        }
        ByteArrayInputStream baIs = new ByteArrayInputStream(sb.toString()
                .getBytes());
        return baIs;

    }

    protected boolean isAKnowCommand(String command) {
        return command.equals("recover");
    }

    public OutputStream writeResource(Object txId, Object resourceId,
            boolean append) throws ResourceManagerException {

        checkForVirtualAdminCommand(resourceId);

        return super.writeResource(txId, resourceId, append);
    }
}
