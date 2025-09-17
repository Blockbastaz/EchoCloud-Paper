package dev.echocloud.Cloud;

public class CloudLogger {
    private boolean debug;

    public CloudLogger(boolean debug) {
        this.debug = debug;
    }

    public void info(String message, Object... args) {
        System.out.println("[INFO] " + format(message, args));
    }

    public void warn(String message, Object... args) {
        System.out.println("[WARN] " + format(message, args));
    }

    public void error(String message, Object... args) {
        System.err.println("[ERROR] " + format(message, args));
    }

    public void debug(String message, Object... args) {
        if (debug) {
            System.out.println("[DEBUG] " + format(message, args));
        }
    }

    private String format(String message, Object... args) {
        if (args == null || args.length == 0) return message;

        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int lastPos = 0;
        int placeholderPos;

        while ((placeholderPos = message.indexOf("{}", lastPos)) != -1 && argIndex < args.length) {
            sb.append(message, lastPos, placeholderPos);
            sb.append(args[argIndex++] != null ? args[argIndex-1].toString() : "null");
            lastPos = placeholderPos + 2;
        }
        sb.append(message.substring(lastPos));
        return sb.toString();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
