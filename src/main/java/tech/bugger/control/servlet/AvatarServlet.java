package tech.bugger.control.servlet;

import java.io.IOException;
import java.io.Serial;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.ProfileService;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;

/**
 * Custom servlet that serves avatars and avatar thumbnails.
 */
public class AvatarServlet extends MediaServlet {

    @Serial
    private static final long serialVersionUID = 3230525044134835918L;

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(AvatarServlet.class);

    /**
     * The resource path to the default avatar.
     */
    private static final String DEFAULT_AVATAR_PATH = "/resources/images/avatar.jpg";

    /**
     * The resource path to the default avatar thumbnail.
     */
    private static final String DEFAULT_THUMBNAIL_PATH = "/resources/images/thumbnail.jpg";

    /**
     * The current application settings.
     */
    @Inject
    private ApplicationSettings applicationSettings;

    /**
     * The current user session.
     */
    @Inject
    private UserSession session;

    /**
     * The post service providing attachments.
     */
    @Inject
    private ProfileService profileService;

    /**
     * Handles a request for a user's avatar. Expects the user's ID or username and the type of avatar (full image or
     * thumbnail) as a request parameter.
     * <p>
     * Verifies if the client is authorized to view the avatar, retrieves it and writes the attachment or potential
     * errors to the response.
     *
     * @param request  The request to handle.
     * @param response The response to return to the client.
     */
    @Override
    protected void handleRequest(final HttpServletRequest request, final HttpServletResponse response) {
        if (!applicationSettings.getConfiguration().isGuestReading() && session.getUser() == null) {
            log.debug("Refusing to serve avatar picture to anonymous user.");
            redirectToNotFoundPage(response);
            return;
        }

        // Retrieve user and image type.
        User user = fetchUser(request);
        if (user == null) {
            log.debug("Invalid user ID or username given.");
            redirectToNotFoundPage(response);
            return;
        }
        boolean serveThumbnail = "thumbnail".equals(request.getParameter("type"));

        // Fetch the requested image.
        byte[] image = serveThumbnail ? user.getAvatarThumbnail() : profileService.getAvatarForUser(user.getId());
        if (image == null || image.length == 0) {
            image = loadDefaultAvatar(serveThumbnail);
        }
        if (image == null) {
            log.debug("Avatar or thumbnail for user with ID " + user.getId() + " not found.");
            redirectToNotFoundPage(response);
            return;
        }

        // Initialize servlet response.
        response.reset();
        configureClientCaching(response);

        // Write image to response.
        try {
            response.getOutputStream().write(image);
        } catch (IOException e) {
            log.warning("Could not write servlet response.", e);
        }
    }

    /**
     * Loads the default avatar or avatar thumbnail.
     *
     * @param serveThumbnail Whether to return the thumbnail or the entire avatar.
     * @return The default avatar or thumbnail.
     */
    private byte[] loadDefaultAvatar(final boolean serveThumbnail) {
        try {
            String path = serveThumbnail ? DEFAULT_THUMBNAIL_PATH : DEFAULT_AVATAR_PATH;
            return getServletContext().getResourceAsStream(path).readAllBytes();
        } catch (IOException e) {
            log.warning("Could not load default avatar or thumbnail.", e);
            return null;
        }
    }

    /**
     * Parses the request parameters and fetches the user identified by them. Users can be specified by their ID (using
     * request parameter {@code id}) or by their username (using request parameter {@code u}). The ID has precedence
     * over the username.
     *
     * @param request The request object to parse the parameters from.
     * @return The user if they could be found, {@code null} otherwise.
     */
    private User fetchUser(final HttpServletRequest request) {
        try {
            return profileService.getUser(Integer.parseInt(request.getParameter("id")));
        } catch (NumberFormatException e) {
            String username = request.getParameter("u");
            return username != null ? profileService.getUserByUsername(username) : null;
        }
    }

}
