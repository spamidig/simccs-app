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
package org.simccs.desktop.ui.login.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.airavata.model.workspace.Notification;
import org.simccs.desktop.connectors.airavata.AiravataManager;
import org.simccs.desktop.ui.commons.SimCCSDialogHelper;
import org.simccs.desktop.util.SimCCSConfig;
import org.simccs.desktop.util.SimCCSContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simccs.gui.SimCCSMain;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;


public class LoginControllerSmall {
    private final static Logger logger = LoggerFactory.getLogger(LoginControllerSmall.class);

    @FXML
    public Label notificationLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button simccsButton;

    @FXML
    private Hyperlink dontHaveAccountLink;

    @FXML
    private WebView loginWebView;

    //Dummy class used for storing notification list index
    private class Index{
        int index;
    }

    public void initialize() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        try{
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = (hostname, session) -> true;

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }catch (Exception ex){
            ex.printStackTrace();
        }

        loginButton.setOnMouseClicked(event -> handleSimCCSWebLogin());

        simccsButton.setOnMouseClicked(event -> SimCCSMain.showSimCCS());

        dontHaveAccountLink.setOnAction(event -> {
            try {
                String url;
                if(SimCCSConfig.DEV){
                    url = "https://dev.simccs.org/create";
                }else{
                    url = "https://simccs.org/create";
                }
//                Desktop.getDesktop().browse(new URI(url));
                loginWebView.getEngine().load(url);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        handleSimCCSWebLogin();

        //initializing notification messages
        notificationLabel.setCursor(Cursor.HAND);
        notificationLabel.setStyle("-fx-border-color: white;");
        notificationLabel.setMaxWidth(Double.MAX_VALUE);
        try{
            java.util.List<Notification> messages = AiravataManager.getInstance().getNotifications();
            final Index index = new Index();
            index.index = 0;
            if (messages != null && messages.size() > 0) {
                notificationLabel.setText(messages.get(index.index).getTitle() + " : "
                        + messages.get(index.index).getNotificationMessage().split("\r|\n")[0]);
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0),
                        event -> {
                            index.index++;
                            index.index = index.index % messages.size();
                            switch (messages.get(index.index).getPriority()){
                                case HIGH:
                                    notificationLabel.setTextFill(Color.web("#ff0000"));
                                    break;
                                case NORMAL:
                                    notificationLabel.setTextFill(Color.web("#ffa500"));
                                    break;
                                case LOW:
                                    notificationLabel.setTextFill(Color.web("#808080"));
                                    break;
                            }
                            notificationLabel.setText(messages.get(index.index).getTitle() + " : "
                                    + messages.get(index.index).getNotificationMessage().split("\r|\n")[0]);

                            notificationLabel.setOnMouseClicked(event1 -> SimCCSDialogHelper.showInformationDialog("Notification", messages.get(index.index).getTitle(),
                                    messages.get(index.index).getNotificationMessage(), null));
                        }),
                        new KeyFrame(Duration.seconds(5)));
                timeline.setCycleCount(Animation.INDEFINITE);
                timeline.play();
            }
        }catch (Exception ex){
            //cannot connect to Airavata
            ex.printStackTrace();
        }
    }

    public boolean handleSimCCSWebLogin(){
        final WebEngine webEngine = loginWebView.getEngine();
        final Label location = new Label();
        String url;
        if(SimCCSConfig.DEV){
            url = "https://dev.simccs.org/login-desktop";
        }else{
            url = "https://simccs.org/login-desktop";
        }


        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {
                String locationUrl = webEngine.getLocation();
                location.setText(locationUrl);
                Map<String, String> params = getQueryMap(locationUrl);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                if(params.containsKey("status")){
                    if(params.get("status").equals("ok")){
                        //login successful
                        String token = params.get("code");
                        String refreshToken = params.get("refresh_code");
                        int validTime = Integer.parseInt(params.get("valid_time").trim());
                        String userName = params.get("username");
                        SimCCSContext.getInstance().setAuthenticated(true);
                        SimCCSContext.getInstance().setOAuthToken(token);
                        SimCCSContext.getInstance().setRefreshToken(refreshToken);
                        SimCCSContext.getInstance().setTokenExpiaryTime(validTime);
                        SimCCSContext.getInstance().setUserName(userName);
                        stage.close();
                    }else if(params.get("status").equals("less_privileged")){
                        //login failed
                        java.net.CookieHandler.setDefault(new com.sun.webkit.network.CookieManager());
                        webEngine.load(url);
                        loginWebView.setVisible(false);
                        SimCCSDialogHelper.showInformationDialog("Login Failed", "Unauthorized login",
                                "You don't have permission to access this client." +
                                        " Please contact the Gateway Admin to get your account authorized by sending an" +
                                        " email to help@simccs.org.", stage);
                        loginWebView.setVisible(true);
                    }else{
                        //login failed
                        java.net.CookieHandler.setDefault(new com.sun.webkit.network.CookieManager());
                        webEngine.load(url);
                        loginWebView.setVisible(false);
                        SimCCSDialogHelper.showInformationDialog("Login Failed", "Unauthorized login",
                                "You don't have permission to access this client." +
                                " Please use a correct user credentials and try again.", stage);
                        loginWebView.setVisible(true);
                    }
                }
            }
        });
        webEngine.load(url);
        return false;
    }

    private Map<String, String> getQueryMap(String query)
    {
        Map<String, String> map = new HashMap<>();
        if(query.contains("?")) {
            String[] params = query.split("\\?")[1].split("&");
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
        }
        return map;
    }

}