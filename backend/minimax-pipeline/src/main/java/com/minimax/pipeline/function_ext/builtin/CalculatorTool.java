package com.minimax.pipeline.function_ext.builtin;

import com.minimax.pipeline.function_ext.executor.ToolFunction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简易计算器: + - * / % 括号 + 数学函数 (sin, cos, tan, sqrt, log, abs, pow, exp, max, min)。
 *
 * 不依赖 ScriptEngine (Java 17 headless 缺 Nashorn), 改用自实现表达式求值。
 * 支持: 数字、小数点、四则运算、取模、括号、一元负号、函数调用、逗号分隔参数。
 */
@Component
public class CalculatorTool implements ToolFunction {
    @Override public String name() { return "calculator"; }

    @Override
    public String execute(Map<String, Object> args) {
        if (args == null) return "{\"error\":\"missing expression\"}";
        Object exprObj = args.get("expression");
        if (exprObj == null) return "{\"error\":\"missing expression\"}";
        String expr = String.valueOf(exprObj).trim();
        if (expr.isEmpty()) return "{\"error\":\"empty expression\"}";

        // 替换函数名为占位符 (用字母数字) 再求值
        // 安全过滤: 只允许数字、小数点、运算符、括号、空白、字母 (函数名)、逗号
        String filtered = expr.replaceAll("[^0-9+\\-*/%().,\\sA-Za-z]", "");
        if (!filtered.equals(expr)) {
            return "{\"error\":\"expression contains invalid characters\",\"original\":\"" + escape(expr) + "\"}";
        }

        try {
            Parser p = new Parser(filtered);
            double result = p.parseExpr();
            p.skipSpaces();
            if (p.pos < p.src.length()) {
                return "{\"error\":\"unexpected char at pos " + p.pos + "\"}";
            }
            if (result == (long) result) {
                return "{\"expression\":\"" + escape(expr) + "\",\"result\":" + (long) result + "}";
            }
            return "{\"expression\":\"" + escape(expr) + "\",\"result\":" + result + "}";
        } catch (Exception e) {
            return "{\"error\":\"evaluation failed: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---- 简单递归下降解析器 ----
    static class Parser {
        final String src;
        int pos;
        Parser(String s) { this.src = s; this.pos = 0; }

        void skipSpaces() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        // expr = term (('+'|'-') term)*
        double parseExpr() {
            skipSpaces();
            double v = parseTerm();
            while (true) {
                skipSpaces();
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                    char op = src.charAt(pos++);
                    double t = parseTerm();
                    v = op == '+' ? v + t : v - t;
                } else break;
            }
            return v;
        }

        // term = factor (('*'|'/'|'%') factor)*
        double parseTerm() {
            skipSpaces();
            double v = parseFactor();
            while (true) {
                skipSpaces();
                if (pos < src.length()) {
                    char c = src.charAt(pos);
                    if (c == '*' || c == '/' || c == '%') {
                        pos++;
                        double f = parseFactor();
                        if (c == '*') v = v * f;
                        else if (c == '/') v = v / f;
                        else v = v % f;
                    } else break;
                } else break;
            }
            return v;
        }

        // factor = ('-' factor) | atom
        double parseFactor() {
            skipSpaces();
            if (pos < src.length() && src.charAt(pos) == '-') {
                pos++;
                return -parseFactor();
            }
            if (pos < src.length() && src.charAt(pos) == '+') {
                pos++;
                return parseFactor();
            }
            return parseAtom();
        }

        // atom = number | '(' expr ')' | function
        double parseAtom() {
            skipSpaces();
            if (pos >= src.length()) throw new RuntimeException("unexpected end");
            char c = src.charAt(pos);
            if (c == '(') {
                pos++;
                double v = parseExpr();
                skipSpaces();
                if (pos >= src.length() || src.charAt(pos) != ')') {
                    throw new RuntimeException("missing )");
                }
                pos++;
                return v;
            }
            if (Character.isDigit(c) || c == '.') return parseNumber();
            if (Character.isLetter(c)) return parseFunction();
            throw new RuntimeException("unexpected char: " + c);
        }

        double parseNumber() {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
            if (pos == start) throw new RuntimeException("expected number");
            return Double.parseDouble(src.substring(start, pos));
        }

        // function = name '(' arglist ')'  e.g. sin(1) max(1,2)
        double parseFunction() {
            int start = pos;
            while (pos < src.length() && Character.isLetter(src.charAt(pos))) pos++;
            String fn = src.substring(start, pos).toLowerCase();
            skipSpaces();
            if (pos >= src.length() || src.charAt(pos) != '(') {
                throw new RuntimeException("expected ( after " + fn);
            }
            pos++;
            skipSpaces();
            // parse args
            double[] args;
            if (pos < src.length() && src.charAt(pos) == ')') {
                pos++;
                args = new double[0];
            } else {
                java.util.List<Double> list = new java.util.ArrayList<>();
                list.add(parseExpr());
                skipSpaces();
                while (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                    list.add(parseExpr());
                    skipSpaces();
                }
                if (pos >= src.length() || src.charAt(pos) != ')') {
                    throw new RuntimeException("missing ) in " + fn);
                }
                pos++;
                args = list.stream().mapToDouble(Double::doubleValue).toArray();
            }
            return applyFunction(fn, args);
        }

        double applyFunction(String fn, double[] args) {
            return switch (fn) {
                case "sin"   -> require1(fn, args, Math::sin);
                case "cos"   -> require1(fn, args, Math::cos);
                case "tan"   -> require1(fn, args, Math::tan);
                case "sqrt"  -> require1(fn, args, Math::sqrt);
                case "log"   -> require1(fn, args, Math::log);
                case "abs"   -> require1(fn, args, Math::abs);
                case "exp"   -> require1(fn, args, Math::exp);
                case "floor" -> require1(fn, args, Math::floor);
                case "ceil"  -> require1(fn, args, Math::ceil);
                case "round" -> require1(fn, args, Math::round);
                case "pow"   -> require2(fn, args, Math::pow);
                case "max"   -> requireMulti(fn, args, this::max);
                case "min"   -> requireMulti(fn, args, this::min);
                default -> throw new RuntimeException("unknown function: " + fn);
            };
        }

        double require1(String fn, double[] a, java.util.function.DoubleUnaryOperator op) {
            if (a.length != 1) throw new RuntimeException(fn + " needs 1 arg");
            return op.applyAsDouble(a[0]);
        }
        double require2(String fn, double[] a, java.util.function.DoubleBinaryOperator op) {
            if (a.length != 2) throw new RuntimeException(fn + " needs 2 args");
            return op.applyAsDouble(a[0], a[1]);
        }
        double requireMulti(String fn, double[] a, java.util.function.DoubleBinaryOperator op) {
            if (a.length < 1) throw new RuntimeException(fn + " needs at least 1 arg");
            double v = a[0];
            for (int i = 1; i < a.length; i++) v = op.applyAsDouble(v, a[i]);
            return v;
        }
        double max(double a, double b) { return Math.max(a, b); }
        double min(double a, double b) { return Math.min(a, b); }
    }
}
