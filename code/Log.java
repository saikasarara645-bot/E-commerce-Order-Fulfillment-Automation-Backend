import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/** Log.java – Handles workflow logging to logs.json and viewing order timelines */
public class Log {

    public static final String RESET = "\u001B[0m";
    public static final String ROSE  = "\u001B[38;5;174m"; // exit/error

    private DataPersistence dp;

    public Log(DataPersistence dp) {
        this.dp = dp;
    }

    private String readWholeFile(String filename) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        try {
            br = new BufferedReader(new FileReader(dp.path(filename)));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            return "";
        } finally {
            try { if (br != null) br.close(); } catch (Exception ex) { }
        }

        return sb.toString().trim();
    }

    private void writeWholeFile(String filename, String content) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(dp.path(filename), false);
            fw.write(content);
        } catch (Exception e) {
            // ignore logging errors to avoid disrupting main flow
        } finally {
            try { if (fw != null) fw.close(); } catch (Exception ex) { }
        }
    }

    private String jsonEscape(String s) {
        if (s == null) return "";

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\\') out.append("\\\\");
            else if (c == '"') out.append("\\\"");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else out.append(c);
        }
        return out.toString();
    }

    private String q(String s) {
        return "\"" + jsonEscape(s) + "\"";
    }

    private int skipSpaces(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
            else break;
        }
        return i;
    }

    private int findValueStart(String obj, String key) {
        String token = "\"" + key + "\"";
        int k = obj.indexOf(token);
        if (k < 0) return -1;

        int colon = obj.indexOf(":", k + token.length());
        if (colon < 0) return -1;

        return skipSpaces(obj, colon + 1);
    }

    private String extractJsonString(String obj, String key) {
        int i = findValueStart(obj, key);
        if (i < 0 || i >= obj.length()) return "";
        if (obj.charAt(i) != '"') return "";

        i++;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        while (i < obj.length()) {
            char c = obj.charAt(i);

            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escape = false;
            } else {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            i++;
        }

        return sb.toString();
    }

    private String[] splitTopLevelObjects(String jsonArrayText) {
        String text = (jsonArrayText == null ? "" : jsonArrayText.trim());

        if (text.startsWith("[")) text = text.substring(1);
        if (text.endsWith("]")) text = text.substring(0, text.length() - 1);

        String[] temp = new String[1000];
        int count = 0;

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        temp[count++] = text.substring(start, i + 1);
                        start = -1;
                    }
                }
            }
        }

        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = temp[i];
        }

        return result;
    }

    /** Append a log entry to logs.json */
    public void write(String orderId, String message) {
        try {
            String existing = readWholeFile("logs.json");
            if (existing.equals("")) existing = "[]";

            String[] oldObjects = splitTopLevelObjects(existing);

            StringBuilder json = new StringBuilder();
            json.append("[\n");

            int written = 0;

            for (int i = 0; i < oldObjects.length; i++) {
                if (oldObjects[i] == null || oldObjects[i].trim().equals("")) continue;

                if (written > 0) json.append(",\n");
                json.append(oldObjects[i]);
                written++;
            }

            if (written > 0) json.append(",\n");

            json.append("  {\n");
            json.append("    \"orderId\": ").append(q(orderId)).append(",\n");
            json.append("    \"message\": ").append(q(message)).append("\n");
            json.append("  }\n");

            json.append("]");

            writeWholeFile("logs.json", json.toString());

        } catch (Exception e) {
            // ignore logging errors to avoid disrupting main flow
        }
    }

    /** Display all log entries for a given Order ID (order timeline) */
    public void viewLogsByOrder(String orderId) {
        try {
            String json = readWholeFile("logs.json");
            if (json.equals("")) {
                System.out.print(ROSE + "No log entries found for Order " + orderId + ".\n" + RESET);
                return;
            }

            String[] objects = splitTopLevelObjects(json);
            boolean foundAny = false;

            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i];

                String savedOrderId = extractJsonString(obj, "orderId");
                String message = extractJsonString(obj, "message");

                if (savedOrderId.equalsIgnoreCase(orderId)) {
                    System.out.print("Order " + savedOrderId + " - " + message + "\n");
                    foundAny = true;
                }
            }

            if (!foundAny) {
                System.out.print(ROSE + "No log entries found for Order " + orderId + ".\n" + RESET);
            }

        } catch (Exception e) {
            System.out.print(ROSE + "Error reading logs.\n" + RESET);
        }
    }
}