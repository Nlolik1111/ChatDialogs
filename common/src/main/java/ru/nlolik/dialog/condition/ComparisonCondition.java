package ru.nlolik.dialog.condition;

import ru.nlolik.dialog.runtime.DialogContext;
import ru.nlolik.dialog.runtime.PlaceholderEngine;

public class ComparisonCondition implements DialogCondition {
    private final String left;
    private final String operator;
    private final String right;

    public ComparisonCondition(String left, String operator, String right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public boolean test(DialogContext context) {
        String leftValue = resolve(left, context);
        String rightValue = resolve(right, context);
        Double leftNumber = parseDouble(leftValue);
        Double rightNumber = parseDouble(rightValue);
        return switch (operator) {
            case "<" -> compareNumbers(leftNumber, rightNumber) < 0;
            case ">" -> compareNumbers(leftNumber, rightNumber) > 0;
            case "<=" -> compareNumbers(leftNumber, rightNumber) <= 0;
            case ">=" -> compareNumbers(leftNumber, rightNumber) >= 0;
            case "==", "=" -> leftValue.equalsIgnoreCase(rightValue);
            case "!=" -> !leftValue.equalsIgnoreCase(rightValue);
            default -> false;
        };
    }

    private static String resolve(String value, DialogContext context) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return PlaceholderEngine.resolve(value, context).trim();
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int compareNumbers(Double left, Double right) {
        if (left == null || right == null) {
            return 0;
        }
        return Double.compare(left, right);
    }
}
