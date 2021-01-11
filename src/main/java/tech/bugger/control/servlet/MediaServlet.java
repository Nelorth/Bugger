package tech.bugger.control.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Custom servlet that serves avatars and avatar thumbnails.
 */
public abstract class MediaServlet extends HttpServlet {

    /**
     * The time in seconds clients should cache content.
     */
    private static final int CACHE_AGE = 10 * 60;

    /**
     * Handles a media request.
     *
     * @param request  The request to handle.
     * @param response The response to return to the client.
     */
    protected abstract void handleRequest(HttpServletRequest request, HttpServletResponse response);

    /**
     * Handles a GET request.
     *
     * @param request  The request to handle.
     * @param response The response to return to the client.
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) {
        handleRequest(request, response);
    }

    /**
     * Handles a POST request.
     *
     * @param request  The request to handle.
     * @param response The response to return to the client.
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) {

        handleRequest(request, response);
    }

    /**
     * Redirects the user to a page that indicates the requested content could not be found.
     *
     * @param response The response to perform the redirect.
     */
    protected void redirectToNotFoundPage(HttpServletResponse response) {
        try {
            // TODO: Redirect to our own error page.
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
        } catch (IOException e) {
            throw new InternalError("Could not redirect to 404 page.");
        }
    }

    /**
     * Adds client caching headers to the response.
     *
     * @param response The response to add headers to.
     */
    protected void enableClientCaching(HttpServletResponse response) {
        long expiry = new Date().getTime() + CACHE_AGE * 1000;
        response.setDateHeader("Expires", expiry);
    }

}
