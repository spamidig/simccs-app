package org.simccs.desktop.ui.commons;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageButton extends Button {
    
    private final String STYLE_NORMAL = "-fx-background-color: transparent; -fx-padding: 5, 5, 5, 5;";
    private final String STYLE_PRESSED = "-fx-background-color: transparent; -fx-padding: 6 4 4 6;";
    
    public ImageButton(String imageurl) {
        setGraphic(new ImageView(new Image(getClass().getResourceAsStream(imageurl))));
        setStyle(STYLE_NORMAL);
        
        setOnMousePressed(event -> setStyle(STYLE_PRESSED));
        
        setOnMouseReleased(event -> setStyle(STYLE_NORMAL));
    }
    
}