package com.agentbackend.tool;

import com.agentbackend.service.ToolCallLogService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Stack;

@Component
public class CalculatorTool {
    
    @Autowired
    private ToolCallLogService toolCallLogService;
    
    @Tool(name = "calculate", description = "用于执行数学计算，当用户请求计算表达式时使用。支持四则运算和括号")
    public String calculate(
            @ToolParam(description = "数学表达式，如: 123 * 456 或 (10 + 20) * 3") String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "{\"success\": false, \"message\": \"表达式不能为空\"}";
        }
        
        String expr = expression.trim();
        
        if (!isValidExpression(expr)) {
            return "{\"success\": false, \"message\": \"表达式包含非法字符\"}";
        }
        
        try {
            double result = evaluate(expr);
            String resultStr = result == (long) result ? String.valueOf((long) result) : String.valueOf(result);
            
            toolCallLogService.logToolCall("calculator", "calculate", 
                java.util.Map.of("expression", expression), 
                "{\"success\": true, \"result\": \"" + resultStr + "\"}");
            
            return "{\"success\": true, \"result\": \"" + resultStr + "\"}";
        } catch (Exception e) {
            String errorResult = "{\"success\": false, \"message\": \"计算失败: " + escapeJson(e.getMessage()) + "\"}";
            toolCallLogService.logToolCallError("calculator", "calculate", 
                java.util.Map.of("expression", expression), e.getMessage());
            return errorResult;
        }
    }
    
    private boolean isValidExpression(String expression) {
        return expression.matches("^[0-9+\\-*/().\\s]+$");
    }
    
    private double evaluate(String expression) {
        expression = expression.replaceAll("\\s+", "");
        return new ExpressionParser().parse(expression);
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static class ExpressionParser {
        private int pos = -1;
        private int ch;
        
        private void nextChar(String str) {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }
        
        private boolean eat(int charToEat, String str) {
            while (ch == ' ') nextChar(str);
            if (ch == charToEat) {
                nextChar(str);
                return true;
            }
            return false;
        }
        
        private double parse(String str) {
            this.pos = -1;
            nextChar(str);
            double x = parseExpression(str);
            if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
            return x;
        }
        
        private double parseExpression(String str) {
            double x = parseTerm(str);
            for (;;) {
                if      (eat('+', str)) x += parseTerm(str);
                else if (eat('-', str)) x -= parseTerm(str);
                else return x;
            }
        }
        
        private double parseTerm(String str) {
            double x = parseFactor(str);
            for (;;) {
                if      (eat('*', str)) x *= parseFactor(str);
                else if (eat('/', str)) x /= parseFactor(str);
                else return x;
            }
        }
        
        private double parseFactor(String str) {
            if (eat('+', str)) return parseFactor(str);
            if (eat('-', str)) return -parseFactor(str);
            
            double x;
            int startPos = this.pos;
            if (eat('(', str)) {
                x = parseExpression(str);
                eat(')', str);
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar(str);
                x = Double.parseDouble(str.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char)ch);
            }
            
            return x;
        }
    }
}
