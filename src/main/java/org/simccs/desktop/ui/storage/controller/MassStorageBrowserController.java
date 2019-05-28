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
package org.simccs.desktop.ui.storage.controller;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.simccs.desktop.connectors.storage.*;
import org.simccs.desktop.ui.commons.SimCCSDialogHelper;
import org.simccs.desktop.ui.storage.model.FileListModel;
import org.simccs.desktop.util.SimCCSContext;
import org.simccs.desktop.util.messaging.SimCCSEvent;
import org.simccs.desktop.util.messaging.SimCCSEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Vector;

public class MassStorageBrowserController {
    private final static Logger logger = LoggerFactory.getLogger(MassStorageBrowserController.class);

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    @FXML
    private TableView<FileListModel> fbLocalFileTable;

    @FXML
    private TableColumn<FileListModel,FileListModel> fbLocalFileTblFileName;

    @FXML
    private TableColumn<FileListModel,String> fbLocalFileTblFileSize;

    @FXML
    private TableColumn<FileListModel,String> fbLocalFileTblLastMod;

    @FXML
    private TableView<FileListModel> fbRemoteFileTable;

    @FXML
    private TableColumn<FileListModel,FileListModel> fbRemoteFileTblFileName;

    @FXML
    private TableColumn<FileListModel,String> fbRemoteFileTblFileSize;

    @FXML
    public TableColumn<FileListModel,String> fbRemoteFileTblLastMod;

    @FXML
    private TextField fbLocalPath;

    @FXML
    private TextField fbRemotePath;

    private Path currentLocalPath;

    private String currentRemotePath;

    ObservableList<FileListModel> currentLocalFileList, currentRemoteFileList;

    @SuppressWarnings("unused")
    public void initialize(){
        try{
            initialiseColumnWidths();
            initializeLocalFileTable();
            initializeRemoteFileTable();

            fbLocalPath.setAlignment(Pos.BASELINE_LEFT);
            fbRemotePath.setAlignment(Pos.BASELINE_LEFT);
        }catch (Exception e){
            SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", null,
                    "Failed opening mass storage browser");
            if(fbLocalPath != null && fbLocalPath.getScene() != null && fbLocalPath.getScene().getWindow() != null)
                ((Stage)fbLocalPath.getScene().getWindow()).close();
        }
    }

    private void initialiseColumnWidths(){
        fbLocalFileTblFileName.prefWidthProperty().bind(fbLocalFileTable.widthProperty().divide(3));
        fbLocalFileTblFileSize.prefWidthProperty().bind(fbLocalFileTable.widthProperty().divide(3));
        fbLocalFileTblLastMod.prefWidthProperty().bind(fbLocalFileTable.widthProperty().divide(3));

        fbRemoteFileTblFileName.prefWidthProperty().bind(fbRemoteFileTable.widthProperty().divide(3));
        fbRemoteFileTblFileSize.prefWidthProperty().bind(fbRemoteFileTable.widthProperty().divide(3));
        fbRemoteFileTblLastMod.prefWidthProperty().bind(fbRemoteFileTable.widthProperty().divide(3));
    }

    private void initializeLocalFileTable(){
        String userHome = System.getProperty("user.home");
        this.currentLocalPath = Paths.get(userHome);
        fbLocalFileTblFileName.setCellValueFactory(cellData-> new SimpleObjectProperty(cellData.getValue()));
        fbLocalFileTblFileName.setCellFactory(param -> new TableCell<FileListModel, FileListModel>(){
          @Override
          public void updateItem(FileListModel item, boolean empty){
              if(item != null){
                  HBox hBox = new HBox(2);
                  Image fileImage;
                  if(item.getFileListModelType().equals(FileListModel.FileListModelType.FILE)){
                      fileImage = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/file.png"));
                  }else{
                      fileImage = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/folder.png"));
                  }
                  hBox.getChildren().add(new javafx.scene.image.ImageView(fileImage));
                  hBox.getChildren().add(new Label(item.getFileName()));
                  setGraphic(hBox);
              }else{
                  setGraphic(null);
              }
          }
        });
        fbLocalFileTblFileSize.setCellValueFactory(cellData -> cellData.getValue().getFileListModelType()
                .equals(FileListModel.FileListModelType.FILE) ? new SimpleStringProperty(cellData.getValue().getSize()+" bytes")
                : null);
        fbLocalFileTblFileSize.setCellFactory(param -> new TableCell<FileListModel, String>(){
            @Override
            public void updateItem(String item, boolean empty){
                setText(item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        fbLocalFileTblLastMod.setCellValueFactory(cellData -> cellData.getValue().getFileListModelType()
                .equals(FileListModel.FileListModelType.PARENT_DIR) ? null
                : new SimpleStringProperty(LocalDateTime.ofEpochSecond(cellData.getValue().getLastModifiedTime() / 1000, 0,
                SimCCSContext.getInstance().getTimeZoneOffset()).toString()));
        fbLocalFileTblLastMod.setCellFactory(param -> new TableCell<FileListModel, String>(){
            @Override
            public void updateItem(String item, boolean empty){
                setText(item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        currentLocalFileList = FXCollections.observableArrayList();
        fbLocalFileTable.setItems(currentLocalFileList);

        fbLocalFileTable.setOnMouseClicked(event -> {
            FileListModel fileListModel = fbLocalFileTable.getSelectionModel().getSelectedItem();
            if(fileListModel != null && event.getClickCount()==2) {
                if (fileListModel.getFileListModelType().equals(FileListModel.FileListModelType.DIR)) {
                    currentLocalPath = Paths.get(currentLocalPath.toString() + File.separator + fileListModel.getFileName());
                } else if (fileListModel.getFileListModelType().equals(FileListModel.FileListModelType
                        .PARENT_DIR)) {
                    currentLocalPath = currentLocalPath.getParent();
                }
                populateLocalFileList();
            }
        });

        fbLocalFileTable.setRowFactory(tv -> {
            TableRow<FileListModel> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && !currentLocalFileList.get(row.getIndex()).getFileName().equals("..")
                        && (currentLocalFileList.get(row.getIndex()).getFileListModelType().equals(FileListModel
                        .FileListModelType.FILE) || (currentLocalFileList.get(row.getIndex()).getFileListModelType()
                        .equals(FileListModel.FileListModelType.DIR)))){
                    FileListModel selectedFileListModel = fbLocalFileTable.getSelectionModel().getSelectedItem();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, selectedFileListModel);
                    db.setContent(cc);
                    event.consume();
                }
            });
            return row;
        });
        fbLocalFileTable.setOnDragOver(event -> {
            fbRemoteFileTable.getScene().setCursor(Cursor.DEFAULT);
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                if (((FileListModel) db.getContent(SERIALIZED_MIME_TYPE)).getFileLocation().equals(FileListModel.FileLocation.REMOTE)) {
                    Image image = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/add.png"));
                    fbRemoteFileTable.getScene().setCursor(new ImageCursor(image));
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            }
        });
        fbLocalFileTable.setOnDragDropped(event -> {
            fbRemoteFileTable.getScene().setCursor(Cursor.DEFAULT);
            Dragboard db = event.getDragboard();
            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                FileListModel draggedFileListModel = (FileListModel) db.getContent(SERIALIZED_MIME_TYPE);
                if(draggedFileListModel.getFileListModelType().equals(FileListModel.FileListModelType.FILE)) {
                    downloadFile(draggedFileListModel.getFilePath(),
                            currentLocalPath.toString() + File.separator + draggedFileListModel.getFileName(), draggedFileListModel);
                }else if(draggedFileListModel.getFileListModelType().equals(FileListModel.FileListModelType.DIR)){
                    downloadDir(draggedFileListModel.getFilePath(),
                            currentLocalPath.toString() + File.separator + draggedFileListModel.getFileName(), draggedFileListModel);
                }
                event.setDropCompleted(true);
                event.consume();
            }
        });

        populateLocalFileList();
    }

    private void populateLocalFileList(){
        currentLocalFileList.clear();
        fbLocalPath.setText(currentLocalPath.toString());
        FileListModel fileListModel;
        if(currentLocalPath.getParent() != null){
            fileListModel = new FileListModel("..", FileListModel.FileListModelType.PARENT_DIR,0,0, FileListModel.FileLocation.LOCAL,
                    currentLocalPath.getParent().toString());
            currentLocalFileList.add(fileListModel);
        }
        File parent = new File(currentLocalPath.toString());
        File[] children = parent.listFiles();
        for(File child : children){
            if(child.getName().equals(".") || child.getName().equals("..")) continue;
            fileListModel = new FileListModel(child.getName(), child.isFile()
                    ? FileListModel.FileListModelType.FILE : FileListModel.FileListModelType.DIR, child.length(),
                    child.lastModified(), FileListModel.FileLocation.LOCAL, currentLocalPath.toString() +
                    File.separator + child.getName());
            currentLocalFileList.add(fileListModel);
        }
    }

    private void initializeRemoteFileTable() throws SftpException, JSchException {
        this.currentRemotePath = "/";

        fbRemoteFileTblFileName.setCellValueFactory(cellData-> new SimpleObjectProperty(cellData.getValue()));
        fbRemoteFileTblFileName.setCellFactory(param -> new TableCell<FileListModel, FileListModel>(){
            @Override
            public void updateItem(FileListModel item, boolean empty){
                if(item != null){
                    HBox hBox = new HBox(2);
                    Image fileImage;
                    if(item.getFileListModelType().equals(FileListModel.FileListModelType.FILE)){
                        fileImage = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/file.png"));
                    }else{
                        fileImage = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/folder.png"));
                    }
                    hBox.getChildren().add(new javafx.scene.image.ImageView(fileImage));
                    hBox.getChildren().add(new Label(item.getFileName()));
                    setGraphic(hBox);
                }else{
                    setGraphic(null);
                }
            }
        });
        fbRemoteFileTblFileSize.setCellValueFactory(cellData -> cellData.getValue().getFileListModelType()
                .equals(FileListModel.FileListModelType.FILE) ? new SimpleStringProperty(cellData.getValue().getSize()+" bytes")
                : null);
        fbRemoteFileTblFileSize.setCellFactory(param -> new TableCell<FileListModel, String>(){
            @Override
            public void updateItem(String item, boolean empty){
                setText(item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        fbRemoteFileTblLastMod.setCellValueFactory(cellData -> cellData.getValue().getFileListModelType()
                .equals(FileListModel.FileListModelType.PARENT_DIR) ? null
                : new SimpleStringProperty(LocalDateTime.ofEpochSecond(cellData.getValue().getLastModifiedTime() / 1000, 0,
                SimCCSContext.getInstance().getTimeZoneOffset()).toString()));
        fbRemoteFileTblLastMod.setCellFactory(param -> new TableCell<FileListModel, String>(){
            @Override
            public void updateItem(String item, boolean empty){
                setText(item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        currentRemoteFileList = FXCollections.observableArrayList();
        fbRemoteFileTable.setItems(currentRemoteFileList);

        fbRemoteFileTable.setOnMouseClicked(event -> {
            FileListModel fileListModel = fbRemoteFileTable.getSelectionModel().getSelectedItem();
            if(fileListModel != null && event.getClickCount()==2) {
                if (fileListModel.getFileListModelType().equals(FileListModel.FileListModelType.DIR)) {
                    currentRemotePath = currentRemotePath + "/" + fileListModel.getFileName();
                } else if (fileListModel.getFileListModelType().equals(FileListModel.FileListModelType
                        .PARENT_DIR)) {
                    currentRemotePath = (new File(currentRemotePath)).getParent();
                }
                try {
                    populateRemoteFileTable();
                } catch (Exception e) {
                    e.printStackTrace();
                    SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", fbRemoteFileTable.getScene().getWindow(),
                            "Failed to load remote directory information");
                }
            }
        });

        fbRemoteFileTable.setRowFactory(tv -> {
            TableRow<FileListModel> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && !currentRemoteFileList.get(row.getIndex()).getFileName().equals("..")
                        && (currentRemoteFileList.get(row.getIndex()).getFileListModelType().equals(FileListModel
                        .FileListModelType.FILE) || (currentRemoteFileList.get(row.getIndex()).getFileListModelType()
                        .equals(FileListModel.FileListModelType.DIR)))) {
                    FileListModel selectedFileListModel = fbRemoteFileTable.getSelectionModel().getSelectedItem();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, selectedFileListModel);
                    db.setContent(cc);
                    event.consume();
                }
            });
            return row;
        });
//        fbRemoteFileTable.setOnDragOver(event -> {
//            fbRemoteFileTable.getScene().setCursor(Cursor.DEFAULT);
//            Dragboard db = event.getDragboard();
//            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
//                if (((FileListModel) db.getContent(SERIALIZED_MIME_TYPE)).getFileLocation().equals(FileListModel.FileLocation.LOCAL)) {
//                    Image image = new Image(MassStorageBrowserController.class.getResourceAsStream("/images/add.png"));
//                    fbRemoteFileTable.getScene().setCursor(new ImageCursor(image));
//                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
//                    event.consume();
//                }
//            }
//        });
//        fbRemoteFileTable.setOnDragDropped(event -> {
//            fbRemoteFileTable.getScene().setCursor(Cursor.DEFAULT);
//            Dragboard db = event.getDragboard();
//            if (db.hasContent(SERIALIZED_MIME_TYPE)) {
//                FileListModel draggedFileListModel = (FileListModel) db.getContent(SERIALIZED_MIME_TYPE);
//                if(draggedFileListModel.getFileListModelType().equals(FileListModel.FileListModelType.FILE)) {
//                    if ((new File(currentRemotePath)).getParent() != null && !(new File(currentRemotePath)).getParent().isEmpty()) {
//                        uploadFile(draggedFileListModel.getFilePath(), currentRemotePath.toString() + "/" + draggedFileListModel
//                                .getFileName(), draggedFileListModel);
//                    } else {
//                        uploadFile(draggedFileListModel.getFilePath(), "/" + draggedFileListModel
//                                .getFileName(), draggedFileListModel);
//                    }
//                }else if(draggedFileListModel.getFileListModelType().equals(FileListModel.FileListModelType.DIR)){
//                    uploadDir(draggedFileListModel.getFilePath(), "/" + draggedFileListModel
//                            .getFileName(), draggedFileListModel);
//                }
//                event.setDropCompleted(true);
//                event.consume();
//            }
//        });

        populateRemoteFileTable();
    }

    private void populateRemoteFileTable() throws JSchException, SftpException {
        currentRemoteFileList.clear();
        fbRemotePath.setText(SimCCSContext.getInstance().getUserName() + currentRemotePath);
        FileListModel fileListModel;
        if((new File(currentRemotePath)).getParent() != null && !(new File(currentRemotePath)).getParent().isEmpty()){
            fileListModel = new FileListModel("..", FileListModel.FileListModelType.PARENT_DIR,0,0, FileListModel.FileLocation.REMOTE,
                    (new File(currentRemotePath).getParent()));
            currentRemoteFileList.add(fileListModel);
        }
        Vector<ChannelSftp.LsEntry> children = StorageManager.getInstance().getDirectoryListing(currentRemotePath);
        for(ChannelSftp.LsEntry lsEntry : children){
            if(lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) continue;
            fileListModel = new FileListModel(lsEntry.getFilename(), !lsEntry.getAttrs().isDir()
                    ? FileListModel.FileListModelType.FILE : FileListModel.FileListModelType.DIR, lsEntry.getAttrs().getSize(),
                    lsEntry.getAttrs().getATime() * 1000L, FileListModel.FileLocation.REMOTE, currentRemotePath
                    + "/" + lsEntry.getFilename());
            currentRemoteFileList.add(fileListModel);
        }
    }

    private void uploadFile(String localFile, String remotePath, FileListModel upldFileModel){
        Service<Boolean> service = new Service<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                try {
                    return new GuiFileUploadTask(remotePath, localFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", fbRemoteFileTable.getScene().getWindow(),
                            "Unable To Connect To File Server !");
                }
                return null;
            }
        };
        SimCCSDialogHelper.showProgressDialog(service, "Progress Dialog", fbRemoteFileTable.getScene().getWindow(),
                "Uploading File " + upldFileModel.getFileName());
        service.setOnFailed((WorkerStateEvent t) -> SimCCSDialogHelper.showExceptionDialogAndWait(service.getException(), "Exception Dialog",
                fbRemoteFileTable.getScene().getWindow(), "File Upload Failed"));
        service.setOnSucceeded((WorkerStateEvent t)->{
            //removing the duplicate file with the same name
            for(int i=0;i<currentRemoteFileList.size();i++){
                if(currentRemoteFileList.get(i).getFileName().equals(upldFileModel.getFileName())){
                    currentRemoteFileList.remove(i);
                }
            }
            upldFileModel.setFileLocation(FileListModel.FileLocation.REMOTE);
            upldFileModel.setFilePath(currentRemotePath +"/"+upldFileModel.getFileName());
            currentRemoteFileList.add(upldFileModel);
            int dropIndex = fbRemoteFileTable.getItems().size();
            fbRemoteFileTable.getSelectionModel().select(dropIndex);
            SimCCSEventBus.getInstance().post(new SimCCSEvent(SimCCSEvent.SimCCSEventType.FILE_UPLOADED,localFile));
        });
        service.start();
    }

    private void uploadDir(String localDir, String remoteDir, FileListModel upldFileModel){
        Service<Boolean> service = new Service<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                try {
                    return new GuiDirUploadTask(remoteDir, localDir);
                } catch (Exception e) {
                    e.printStackTrace();
                    SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", fbRemoteFileTable.getScene().getWindow(),
                            "Unable To Connect To File Server !");
                }
                return null;
            }
        };
        SimCCSDialogHelper.showProgressDialog(service, "Progress Dialog", fbRemoteFileTable.getScene().getWindow(),
                "Uploading Directory " + upldFileModel.getFileName());
        service.setOnFailed((WorkerStateEvent t) -> SimCCSDialogHelper.showExceptionDialogAndWait(service.getException(), "Exception Dialog",
                fbRemoteFileTable.getScene().getWindow(), "Directory Upload Failed"));
        service.setOnSucceeded((WorkerStateEvent t)->{
            //removing the duplicate file with the same name
            for(int i=0;i<currentRemoteFileList.size();i++){
                if(currentRemoteFileList.get(i).getFileName().equals(upldFileModel.getFileName())){
                    currentRemoteFileList.remove(i);
                }
            }
            upldFileModel.setFileLocation(FileListModel.FileLocation.REMOTE);
            upldFileModel.setFilePath(currentRemotePath +"/"+upldFileModel.getFileName());
            currentRemoteFileList.add(upldFileModel);
            int dropIndex = fbRemoteFileTable.getItems().size();
            fbRemoteFileTable.getSelectionModel().select(dropIndex);
            SimCCSEventBus.getInstance().post(new SimCCSEvent(SimCCSEvent.SimCCSEventType.FILE_UPLOADED,localDir));
        });
        service.start();
    }

    private void downloadFile(String remoteFile, String localFile, FileListModel downFileModel){
        Service<Boolean> service = new Service<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                try {
                    return new GuiFileDownloadTask(remoteFile, localFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", fbLocalFileTable.getScene().getWindow(),
                            "Unable To Connect To File Server !");
                }
                return null;
            }
        };
        SimCCSDialogHelper.showProgressDialog(service, "Progress Dialog", fbLocalFileTable.getScene().getWindow(),
                "Downloading File " + downFileModel.getFileName());
        service.setOnFailed((WorkerStateEvent t) -> SimCCSDialogHelper.showExceptionDialogAndWait(service.getException(), "Exception Dialog",
                fbLocalFileTable.getScene().getWindow(), "File Download Failed"));
        service.setOnSucceeded((WorkerStateEvent t)->{
            //removing the duplicate file with the same name
            for(int i=0;i<currentLocalFileList.size();i++){
                if(currentLocalFileList.get(i).getFileName().equals(downFileModel.getFileName())){
                    currentLocalFileList.remove(i);
                }
            }
            downFileModel.setFileLocation(FileListModel.FileLocation.LOCAL);
            downFileModel.setFilePath(currentLocalPath.toString()+File.separator+downFileModel.getFileName());
            currentLocalFileList.add(downFileModel);
            int dropIndex = fbLocalFileTable.getItems().size();
            fbLocalFileTable.getSelectionModel().select(dropIndex);
            SimCCSEventBus.getInstance().post(new SimCCSEvent(SimCCSEvent.SimCCSEventType.FILE_DOWNLOADED,localFile));
        });
        service.start();
    }

    private void downloadDir(String remoteDir, String localDir, FileListModel downFileModel){
        Service<Boolean> service = new Service<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                try {
                    return new GuiDirDownloadTask(remoteDir, localDir);
                } catch (Exception e) {
                    e.printStackTrace();
                    SimCCSDialogHelper.showExceptionDialogAndWait(e, "Exception Dialog", fbLocalFileTable.getScene().getWindow(),
                            "Unable To Connect To File Server !");
                }
                return null;
            }
        };
        SimCCSDialogHelper.showProgressDialog(service, "Progress Dialog", fbLocalFileTable.getScene().getWindow(),
                "Downloading Directory " + downFileModel.getFileName());
        service.setOnFailed((WorkerStateEvent t) -> SimCCSDialogHelper.showExceptionDialogAndWait(service.getException(), "Exception Dialog",
                fbLocalFileTable.getScene().getWindow(), "Directory Download Failed"));
        service.setOnSucceeded((WorkerStateEvent t)->{
            //removing the duplicate file with the same name
            for(int i=0;i<currentLocalFileList.size();i++){
                if(currentLocalFileList.get(i).getFileName().equals(downFileModel.getFileName())){
                    currentLocalFileList.remove(i);
                }
            }
            downFileModel.setFileLocation(FileListModel.FileLocation.LOCAL);
            downFileModel.setFilePath(currentLocalPath.toString()+File.separator+downFileModel.getFileName());
            currentLocalFileList.add(downFileModel);
            int dropIndex = fbLocalFileTable.getItems().size();
            fbLocalFileTable.getSelectionModel().select(dropIndex);
            SimCCSEventBus.getInstance().post(new SimCCSEvent(SimCCSEvent.SimCCSEventType.FILE_DOWNLOADED,localDir));
        });
        service.start();
    }

    public void gotoRemoteDir(String path) throws JSchException, SftpException {
        currentRemoteFileList.clear();
        currentRemotePath = path;
        fbRemotePath.setText(SimCCSContext.getInstance().getUserName() + currentRemotePath);
        FileListModel fileListModel;
        if((new File(currentRemotePath)).getParent() != null && !(new File(currentRemotePath)).getParent().isEmpty()){
            fileListModel = new FileListModel("..", FileListModel.FileListModelType.PARENT_DIR,0,0, FileListModel.FileLocation.REMOTE,
                    (new File(currentRemotePath).getParent()));
            currentRemoteFileList.add(fileListModel);
        }
        Vector<ChannelSftp.LsEntry> children = StorageManager.getInstance().getDirectoryListing(currentRemotePath);
        for(ChannelSftp.LsEntry lsEntry : children){
            if(lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) continue;
            fileListModel = new FileListModel(lsEntry.getFilename(), !lsEntry.getAttrs().isDir()
                    ? FileListModel.FileListModelType.FILE : FileListModel.FileListModelType.DIR, lsEntry.getAttrs().getSize(),
                    lsEntry.getAttrs().getATime() * 1000L, FileListModel.FileLocation.REMOTE, currentRemotePath
                    + "/" + lsEntry.getFilename());
            currentRemoteFileList.add(fileListModel);
        }
    }
}