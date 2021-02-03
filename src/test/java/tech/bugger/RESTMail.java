package tech.bugger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for system tests with e-mail sending, powered by <a href="http://restmail.net">RESTMail</a>.
 */
public final class RESTMail {

    /**
     * The base URL for RESTMail inboxes.
     */
    private static final String BASE_URL = "http://restmail.net/mail/";

    /**
     * Regex pattern matching the last URL in a RESTMail JSON array.
     */
    private static final Pattern LAST_URL_PAT = Pattern.compile("[sS]*\"text\":.*?(https?://[^\",]*?)[\\s\\\\n].*?\",");

    /**
     * Prevents instantiation of this utility class.
     */
    private RESTMail() {
        throw new UnsupportedOperationException(); // for reflection abusers
    }

    /**
     * Retrieves all e-mails of the user {@code username}.
     *
     * @param username The user whose RESTMail inbox to query.
     * @return The e-mails of {@code username} as JSON array.
     */
    public static String retrieveEmails(final String username) {
        try (InputStream is = new URL(BASE_URL + username).openStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Error when retrieving e-mails of user " + username);
        }
    }

    /**
     * Clears all e-mails of the user {@code username}.
     *
     * @param username The user whose RESTMail inbox to evacuate.
     * @return {@code true} iff cleanup was successful.
     */
    public static boolean clearEmails(final String username) {
        try {
            URL url = new URL(BASE_URL + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            throw new AssertionError("Error when clearing e-mails of user " + username);
        }
    }

    /**
     * Extracts the latest URL in the bodies of e-mails of the user {@code username}.
     *
     * @param username The user whose RESTMail inbox to crawl through.
     * @return The latest confirmation URL for {@code username}.
     */
    public static String getLatestURL(final String username) {
        Matcher matcher = LAST_URL_PAT.matcher(retrieveEmails(username));
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new AssertionError("No registration URL in e-mails of user " + username);
        }
    }

}
