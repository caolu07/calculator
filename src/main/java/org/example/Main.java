package org.example;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main {

    public static void main(String[] args) {
        // 通过普通的启动类启动 JavaFX，避免 IDE 直接运行时报
        // “缺少 JavaFX 运行时组件”。Maven 会将 JavaFX 依赖添加到类路径。
        Application.launch(CalculatorApp.class, args);
    }

    public static class CalculatorApp extends Application {

        // 数字键样式
        private static final String NUMBER_BUTTON_STYLE =
                "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: #dcdcdc;" +
                "-fx-border-radius: 8;" +
                "-fx-font-size: 20px;";

        // 运算符键样式
        private static final String OPERATOR_BUTTON_STYLE =
                "-fx-background-color: #e8f0fe;" +
                "-fx-text-fill: #1a56db;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;";

        // 归零键样式
        private static final String CLEAR_BUTTON_STYLE =
                "-fx-background-color: #ffe8e8;" +
                "-fx-text-fill: #c62828;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;";

        // 等号键样式
        private static final String EQUALS_BUTTON_STYLE =
                "-fx-background-color: #1a56db;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-background-radius: 8;" +
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;";

        @Override
        public void start(Stage stage) {
            // ===== 顶部显示区：显示输入的内容和计算结果 =====
            TextField display = new TextField();
            display.setPromptText("0");
            display.setAlignment(Pos.CENTER_RIGHT);
            display.setEditable(false);          // 目前只用于显示，输入通过按钮完成
            display.setFocusTraversable(false);  // 避免点击按钮后文本框抢焦点
            display.setPrefHeight(70);
            display.setStyle(
                    "-fx-background-color: #ffffff;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-color: #d0d0d0;" +
                    "-fx-border-radius: 8;" +
                    "-fx-font-size: 24px;" +
                    "-fx-padding: 0 12 0 12;");

            // ===== 按钮区 =====
            GridPane buttonGrid = new GridPane();
            buttonGrid.setHgap(8);
            buttonGrid.setVgap(8);
            VBox.setVgrow(buttonGrid, Priority.ALWAYS);

            // 4 列 × 5 行，每列/每行均分剩余空间
            for (int i = 0; i < 4; i++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(25);
                column.setFillWidth(true);
                buttonGrid.getColumnConstraints().add(column);
            }
            for (int i = 0; i < 5; i++) {
                RowConstraints row = new RowConstraints();
                row.setPercentHeight(20);
                row.setFillHeight(true);
                buttonGrid.getRowConstraints().add(row);
            }

            // 前三行：数字 7 8 9 / 4 5 6 * 1 2 3 -
            String[][] keyRows = {
                    {"7", "8", "9", "/"},
                    {"4", "5", "6", "*"},
                    {"1", "2", "3", "-"},
            };
            for (int row = 0; row < keyRows.length; row++) {
                for (int col = 0; col < keyRows[row].length; col++) {
                    String key = keyRows[row][col];
                    buttonGrid.add(createButton(key, isOperator(key)
                            ? OPERATOR_BUTTON_STYLE : NUMBER_BUTTON_STYLE), col, row);
                }
            }

            // 第四行：归零、0、小数点、+
            buttonGrid.add(createButton("C", CLEAR_BUTTON_STYLE), 0, 3);
            buttonGrid.add(createButton("0", NUMBER_BUTTON_STYLE), 1, 3);
            buttonGrid.add(createButton(".", NUMBER_BUTTON_STYLE), 2, 3);
            buttonGrid.add(createButton("+", OPERATOR_BUTTON_STYLE), 3, 3);

            // 第五行：等号占满整行
            Button equalsButton = createButton("=", EQUALS_BUTTON_STYLE);
            GridPane.setColumnSpan(equalsButton, 4);
            buttonGrid.add(equalsButton, 0, 4);

            // ===== 整体布局：文本框在上，按钮区在下 =====
            VBox root = new VBox(12, display, buttonGrid);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: #f5f5f5;");

            Scene scene = new Scene(root, 400, 560);

            stage.setTitle("计算器");
            stage.setMinWidth(320);
            stage.setMinHeight(480);
            stage.setScene(scene);
            stage.show();
        }

        /** 创建键盘按钮，并让它填满所在的网格单元格。 */
        private Button createButton(String text, String style) {
            Button button = new Button(text);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMaxHeight(Double.MAX_VALUE);
            button.setFocusTraversable(false);
            button.setStyle(style);
            button.setCursor(Cursor.HAND);

            // 鼠标悬停：外发光 + 轻微放大，突出当前按钮
            button.setOnMouseEntered(event -> {
                button.setStyle(style + glowStyleFor(style));
                button.toFront();   // 让发光效果盖在相邻按钮之上，不会被遮住
                animateScale(button, 1.07);
            });
            button.setOnMouseExited(event -> {
                button.setStyle(style);
                animateScale(button, 1.0);
            });
            return button;
        }

        /** 根据按键类型返回悬停时的外发光样式：归零键为红色光晕，其余为蓝色光晕。 */
        private String glowStyleFor(String baseStyle) {
            String glowColor = CLEAR_BUTTON_STYLE.equals(baseStyle)
                    ? "rgba(198, 40, 40, 0.9)"
                    : "rgba(37, 99, 235, 0.9)";
            return "-fx-effect: dropshadow(gaussian, " + glowColor + ", 26, 0.45, 0, 0);";
        }

        /** 平滑地把按钮缩放到指定倍率。 */
        private void animateScale(Button button, double targetScale) {
            ScaleTransition transition =
                    new ScaleTransition(Duration.millis(120), button);
            transition.setToX(targetScale);
            transition.setToY(targetScale);
            transition.setInterpolator(Interpolator.EASE_BOTH);
            transition.play();
        }

        private boolean isOperator(String key) {
            return "+".equals(key) || "-".equals(key) || "*".equals(key) || "/".equals(key);
        }
    }
}
