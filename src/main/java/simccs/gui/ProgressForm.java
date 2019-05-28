package simccs.gui;

import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class ProgressForm {
    private Stage dialogStage;
    private ProgressBar pb = new ProgressBar();
    private ProgressIndicator pin = new ProgressIndicator();
    private String progressTitle;

    public ProgressForm(StageStyle stageStyle, Modality modality) {
        dialogStage = new Stage();
        dialogStage.initStyle(stageStyle);
        dialogStage.initModality(modality);
        dialogStage.setResizable(false);
        dialogStage.setWidth(180);
        dialogStage.setMinWidth(180);
        dialogStage.setMaxWidth(180);
        dialogStage.setHeight(70);
        dialogStage.setMinHeight(70);
        dialogStage.setMaxHeight(70);

        pb.setProgress(-1F);
        pin.setProgress(-1F);

        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(pb, pin);

        Scene scene = new Scene(hb);
        dialogStage.setScene(scene);
    }

    public void activateProgressBar(final Task<?>[] tasks)  {
//        ProgressBar pbTemp = new ProgressBar();
//        pbTemp.progressProperty().bind(taskMIP.progressProperty().multiply(0.5));
//        pb.progressProperty().bind(pbTemp.progressProperty().add(taskResult.progressProperty().multiply(0.5)));
        pb.progressProperty().bind(new DoubleBinding() {
            {
                for (Task<?> task:tasks) {
                    super.bind(task.progressProperty());
                }
            }
            @Override
            public double computeValue() {
                double sum = 0;
                for (Task<?> task:tasks) {
                    sum += Math.max(task.progressProperty().getValue(), 0);
                }
                return sum / tasks.length;
            }
        });
        pin.progressProperty().bind(pb.progressProperty());
        dialogStage.show();
    }

    public Stage getDialogStage() {
        return dialogStage;
    }

    public void setProgressTitle(String title) {
        this.progressTitle = title;
        dialogStage.setTitle(progressTitle);
    }
}