package com.github.nr4bt.wasp;

/**
 * @author Orhan Obut
 */
public class WaspError {

    private final String errorMessage;
    private final int statusCode;
    private final String url;

    public WaspError(String url, String errorMessage, int statusCode) {
        this.url = url;
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        if (errorMessage == null) {
            return "";
        }
        return errorMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Wasp Error:");
        if (errorMessage != null) {
            builder.append(", Message:").append(errorMessage);
        }
        builder.append("Status Code: ").append(statusCode)
                .append(" for ").append(url);
        return builder.toString();
    }
}
