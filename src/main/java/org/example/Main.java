package org.example;

import java.math.BigDecimal;
import java.math.MathContext;

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

        // ===== 计算状态 =====
        /** 当前待计算的运算符；null 表示还没有按下过运算符。 */
        private String pendingOperator;
        /** 第一个操作数。 */
        private double firstOperand;
        /** 是否正在等待输入第二个操作数（已按运算符、还没输入第二个数字）。 */
        private boolean waitingForSecondOperand;
        /** 文本框当前显示的是否是刚计算出的结果。 */
        private boolean resultShown;
        /** 正在输入的数字（文本形式），和运算符一起显示在文本框中。 */
        private String currentInput = "0";

        @Override
        public void start(Stage stage) {
            // ===== 顶部显示区：显示输入的内容和计算结果 =====
            TextField display = new TextField();
            display.setText("0");                // 没有任何输入时，文本框中显示 0
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
                            ? OPERATOR_BUTTON_STYLE : NUMBER_BUTTON_STYLE, display), col, row);
                }
            }

            // 第四行：归零、0、小数点、+
            buttonGrid.add(createButton("C", CLEAR_BUTTON_STYLE, display), 0, 3);
            buttonGrid.add(createButton("0", NUMBER_BUTTON_STYLE, display), 1, 3);
            buttonGrid.add(createButton(".", NUMBER_BUTTON_STYLE, display), 2, 3);
            buttonGrid.add(createButton("+", OPERATOR_BUTTON_STYLE, display), 3, 3);

            // 第五行：等号占满整行
            Button equalsButton = createButton("=", EQUALS_BUTTON_STYLE, display);
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

        /** 创建键盘按钮，绑定点击处理，并让它填满所在的网格单元格。 */
        private Button createButton(String text, String style, TextField display) {
            Button button = new Button(text);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMaxHeight(Double.MAX_VALUE);
            button.setFocusTraversable(false);
            button.setStyle(style);
            button.setCursor(Cursor.HAND);

            // 点击按钮时统一交给 handleButtonClick 分发处理
            button.setOnAction(event -> handleButtonClick(text, display));

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

        /** 根据按钮上的文字分发点击处理：数字、小数点、运算符、等号和归零。 */
        private void handleButtonClick(String key, TextField display) {
            if (key.matches("[0-9]")) {
                handleDigit(display, key);
            } else if (".".equals(key)) {
                handleDecimalPoint(display);
            } else if (isOperator(key)) {
                handleOperator(display, key);
            } else if ("=".equals(key)) {
                handleEquals(display);
            } else if ("C".equals(key)) {
                handleClear(display);
            }
        }

        /** 处理数字键：根据当前状态决定是追加数字，还是开始输入新的操作数。 */
        private void handleDigit(TextField display, String digit) {
            if (resultShown) {
                // 上一次的结果已显示：按数字键表示开始一次全新的计算
                resetCalculation();
                startNewNumber(display, digit);
                return;
            }

            if (waitingForSecondOperand) {
                // 第二个操作数开始输入，接在“第一个操作数 + 运算符”的后面显示
                waitingForSecondOperand = false;
                startNewNumber(display, digit);
                return;
            }

            appendDigit(display, digit);
        }

        /** 处理小数点键：同一个操作数中最多只能有一个小数点。 */
        private void handleDecimalPoint(TextField display) {
            if (resultShown) {
                // 上一次的结果已显示：按小数点表示开始一次全新的计算
                resetCalculation();
                currentInput = "0.";
                display.setText(expressionPrefix() + currentInput);
                return;
            }

            if (waitingForSecondOperand) {
                // 第二个操作数从 “0.” 开始输入
                waitingForSecondOperand = false;
                currentInput = "0.";
                display.setText(expressionPrefix() + currentInput);
                return;
            }

            // 当前操作数已经有小数点时，重复点击不再追加
            if (currentInput.contains(".")) {
                return;
            }

            currentInput += ".";
            display.setText(expressionPrefix() + currentInput);
        }

        /**
         * 处理运算符键：
         * 1. 还没有待计算的运算符时，当前显示的数就是第一个操作数；
         * 2. 还没输入第二个操作数时再次按运算符，只覆盖之前的符号；
         * 3. 已经输入第二个操作数时再次按运算符，先完成第一组计算，
         *    并把结果作为第二组计算的第一个操作数。
         */
        private void handleOperator(TextField display, String operator) {
            if (pendingOperator == null) {
                // 当前输入的数据作为第一个操作数，随后在显示框中接上运算符
                firstOperand = parseCurrentInput();
                pendingOperator = operator;
                waitingForSecondOperand = true;
                resultShown = false;
                display.setText(expressionPrefix());
                return;
            }

            if (waitingForSecondOperand) {
                // 第二个操作数还没输入，再次点击运算符只覆盖原来的符号
                pendingOperator = operator;
                display.setText(expressionPrefix());
                return;
            }

            // 第二个操作数已经输入：先算出第一组结果，再以它为第一个操作数继续
            double firstResult = calculate(firstOperand, pendingOperator, parseCurrentInput());
            if (!Double.isFinite(firstResult)) {
                display.setText(formatNumber(firstResult));
                resetCalculation();
                resultShown = true;
                return;
            }
            firstOperand = firstResult;
            pendingOperator = operator;
            waitingForSecondOperand = true;
            resultShown = false;
            display.setText(expressionPrefix());
        }

        /** 处理等号键：只有运算符和第二个操作数都就绪时才计算并显示结果。 */
        private void handleEquals(TextField display) {
            if (pendingOperator == null || waitingForSecondOperand) {
                return;
            }

            double secondOperand = parseCurrentInput();
            double result = calculate(firstOperand, pendingOperator, secondOperand);
            String resultText = formatNumber(result);
            display.setText(resultText);

            resetCalculation();
            // 结果继续留在文本框中，下一次按数字键会开始新的计算
            currentInput = resultText;
            resultShown = true;
        }

        /** 处理归零键：清空当前输入和所有计算状态，回到初始状态。 */
        private void handleClear(TextField display) {
            resetCalculation();
            display.setText("0");
        }

        /** 把用户点击的数字追加到当前正在输入的数末尾；前导 0 最多只保留一个。 */
        private void appendDigit(TextField display, String digit) {
            // 当前只显示一个 0 时：继续按 0 只保留一个 0；按 1~9 则替换这个前导 0。
            if ("0".equals(currentInput)) {
                currentInput = "0".equals(digit) ? "0" : digit;
            } else {
                currentInput += digit;
            }
            display.setText(expressionPrefix() + currentInput);
        }

        /** 开始输入一个新的数字（替换正在输入的数），并刷新文本框显示。 */
        private void startNewNumber(TextField display, String digit) {
            currentInput = "0".equals(digit) ? "0" : digit;
            display.setText(expressionPrefix() + currentInput);
        }

        /** 运算符前面的表达式部分，例如 “12+”；还没按过运算符时为空。 */
        private String expressionPrefix() {
            return pendingOperator == null ? "" : formatNumber(firstOperand) + pendingOperator;
        }

        /** 把正在输入的数解析成数字；错误提示等无法解析的内容按 0 处理。 */
        private double parseCurrentInput() {
            try {
                return Double.parseDouble(currentInput);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }

        /** 根据运算符完成一次四则运算；除以 0 时返回 NaN，由调用方显示错误。 */
        private double calculate(double leftOperand, String operator, double rightOperand) {
            return switch (operator) {
                case "+" -> leftOperand + rightOperand;
                case "-" -> leftOperand - rightOperand;
                case "*" -> leftOperand * rightOperand;
                case "/" -> rightOperand == 0 ? Double.NaN : leftOperand / rightOperand;
                default -> rightOperand;
            };
        }

        /** 把计算结果格式化成适合显示的文字：去掉多余的小数位和末尾的 0。 */
        private String formatNumber(double value) {
            if (!Double.isFinite(value)) {
                return "错误";
            }
            if (value == 0) {
                return "0";
            }
            BigDecimal decimal = BigDecimal.valueOf(value)
                    .round(new MathContext(12))
                    .stripTrailingZeros();
            return decimal.toPlainString();
        }

        /** 清空内部的计算状态，让下一次输入重新开始。 */
        private void resetCalculation() {
            pendingOperator = null;
            firstOperand = 0;
            waitingForSecondOperand = false;
            resultShown = false;
            currentInput = "0";
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
