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
package org.simccs.desktop.ui.experiment.create;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.airavata.model.experiment.ExperimentModel;
import org.apache.thrift.TException;
import org.simccs.desktop.ui.experiment.create.controller.ExperimentCreateController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class ExperimentCreateWindow extends Application{
    private final static Logger logger = LoggerFactory.getLogger(ExperimentCreateWindow.class);

    private static Stage createPrimaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/experiment/create/experiment-create.fxml"));
        primaryStage.setTitle("SimCCS Desktop Client - Create Experiment");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static void displayCreateExperiment() throws IOException {
        if(createPrimaryStage == null || !createPrimaryStage.isShowing()) {
            createPrimaryStage = new Stage();
            Parent root = FXMLLoader.load(ExperimentCreateWindow.class.getResource("/views/experiment/create/experiment-create.fxml"));
            createPrimaryStage.setTitle("SimCCS Desktop Client - Create Experiment");
            createPrimaryStage.setScene(new Scene(root, 800, 600));
            createPrimaryStage.initModality(Modality.WINDOW_MODAL);
            createPrimaryStage.show();
        }
        createPrimaryStage.requestFocus();
    }

    public static void displayEditExperiment(ExperimentModel experimentModel) throws IOException, TException, URISyntaxException {
        if(createPrimaryStage != null) {
            createPrimaryStage.close();
        }
        createPrimaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(ExperimentCreateWindow.class.getResource(
                "/views/experiment/create/experiment-create.fxml"));
        Parent root = loader.load();
        createPrimaryStage.setTitle("SimCCS Desktop Client - Edit Experiment");
        createPrimaryStage.setScene(new Scene(root, 800, 600));
        ExperimentCreateController controller = loader.getController();
        controller.initExperimentEdit(experimentModel);
        createPrimaryStage.initModality(Modality.WINDOW_MODAL);
        createPrimaryStage.show();
    }

    public static void displayCreateSingleCplexExp(String mpsInput) throws IOException, TException, URISyntaxException {
        if(createPrimaryStage != null) {
            createPrimaryStage.close();
        }
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(ExperimentCreateWindow.class.getResource(
                "/views/experiment/create/experiment-create.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("SimCCS Desktop Client - Create Single Cplex Experiment");
        primaryStage.setScene(new Scene(root, 800, 600));
        ExperimentCreateController controller = loader.getController();
        controller.initSingleCplexExperiment(mpsInput);
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.show();
    }

    public static void displayCreateEnsembleCplexExp(String mpsZipInput) throws IOException, TException, URISyntaxException {
        if(createPrimaryStage != null) {
            createPrimaryStage.close();
        }
        Stage primaryStage = new Stage();
        FXMLLoader loader = new FXMLLoader(ExperimentCreateWindow.class.getResource(
                "/views/experiment/create/experiment-create.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("SimCCS Desktop Client - Create Cplex Ensemble Experiment");
        primaryStage.setScene(new Scene(root, 800, 600));
        ExperimentCreateController controller = loader.getController();
        controller.initEnsembleCplexExperiment(mpsZipInput);
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}