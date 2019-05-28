/*
 *
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
 *
*/
package org.simccs.desktop.connectors.storage;

import com.jcraft.jsch.*;
import javafx.concurrent.Task;
import org.simccs.desktop.util.SimCCSContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class GuiFileTask extends Task<Boolean> {
    private final static Logger logger = LoggerFactory.getLogger(GuiFileTask.class);

    protected static final int BUFFER_SIZE = 512000;

    protected Session session = null;
    protected Channel channel = null;
    protected ChannelSftp channelSftp = null;

    public GuiFileTask() throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(SimCCSContext.getInstance().getUserName(),
                SimCCSContext.getInstance().getSFTPHost(), SimCCSContext.getInstance().getSFTPPort());
        session.setPassword(SimCCSContext.getInstance().getOAuthToken());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        channel = session.openChannel("sftp");
        channel.connect();
        channelSftp = (ChannelSftp) channel;
    }

}