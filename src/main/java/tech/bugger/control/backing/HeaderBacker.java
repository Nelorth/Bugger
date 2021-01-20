package tech.bugger.control.backing;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.config.mapping.UrlMapping;
import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.internal.UserSession;
import tech.bugger.business.service.SearchService;
import tech.bugger.global.transfer.User;
import tech.bugger.global.util.Log;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Backing Bean for the header.
 */
@RequestScoped
@Named
public class HeaderBacker implements Serializable {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(HeaderBacker.class);

    @Serial
    private static final long serialVersionUID = 7342292657804667855L;

    /**
     * The user behind the current UserSession.
     */
    private User user;

    /**
     * {@code true} if the Menu should be displayed, {@code false} otherwise.
     */
    private boolean displayMenu;

    /**
     * The current search query.
     */
    private String search;

    /**
     * The userSearchSuggestion.
     */
    private List<String> userSearchSuggestion;

    /**
     * The topicSearchSuggestion.
     */
    private List<String> topicSearchSuggestion;

    /**
     * The reportSearchSuggestion.
     */
    private List<String> reportSearchSuggestion;

    /**
     * The current application settings.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * The current user session.
     */
    private final UserSession session;

    /**
     * The SearchService for Suggestions in the search bar.
     */
    private final SearchService searchService;

    /**
     * The current {@link FacesContext} of the application.
     */
    private final FacesContext fctx;

    /**
     * Constructs a new header backing bean.
     *
     * @param applicationSettings The current application settings.
     * @param searchService       The SearchService for the backer.
     * @param session             The currently active {@link UserSession}.
     * @param fctx                The current {@link FacesContext} of the application.
     */
    @Inject
    public HeaderBacker(final ApplicationSettings applicationSettings, final SearchService searchService,
                        final UserSession session, final FacesContext fctx) {
        this.applicationSettings = applicationSettings;
        this.searchService = searchService;
        this.session = session;
        this.fctx = fctx;
    }

    /**
     * Initializes the User for the header and makes sure the headerMenu is closed.
     */
    @PostConstruct
    void init() {
        user = session.getUser();
        userSearchSuggestion = new ArrayList<>();
        topicSearchSuggestion = new ArrayList<>();
        reportSearchSuggestion = new ArrayList<>();
        displayMenu = Boolean.parseBoolean(fctx.getExternalContext().getRequestParameterMap().get("d"));
    }

    /**
     * Logs out the user and redirects to the homepage.
     *
     * @return {@code pretty:home}
     */
    public String logout() {
        log.debug("Logout called for user " + session.getUser() + ".");
        session.setUser(null);
        session.invalidateSession();
        return "pretty:home";
    }

    /**
     * Takes the user to the search page with the current {@code search} already typed in.
     *
     * @return The location to redirect to.
     */
    public String executeSearch() throws IOException {
        try {
            ExternalContext ectx = fctx.getExternalContext();
            ectx.redirect(ectx.getRequestContextPath() + "/search?q=" + search);
        } catch (IOException e) {
            redirectTo404Page();
        }
        return null;
    }

    /**
     * Redirects the user to a 404 page.
     */
    private void redirectTo404Page() {
        try {
            ExternalContext ectx = fctx.getExternalContext();
            ectx.redirect(ectx.getRequestContextPath() + "/error");
        } catch (IOException e) {
            throw new InternalError("Redirection to error page failed.");
        }
    }

    /**
     * @return The user.
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user The user to set.
     */
    public void setUser(final User user) {
        this.user = user;
    }

    /**
     * Activates/Deactivates the menu.
     *
     * @return {@code null} to reload the page.
     */
    public String toggleMenu() {
        if (displayMenu) {
            closeMenu();
        } else {
            openMenu();
        }
        return null;
    }

    /**
     * Determine alert class for messages.
     *
     * @return The determined alert class.
     */
    public String determineAlertClass() {
        if (!fctx.getMessageList(null).isEmpty()) {
            FacesMessage.Severity maxSeverity = fctx.getMessageList().stream().map(FacesMessage::getSeverity)
                    .max(FacesMessage.Severity::compareTo).orElseThrow();
            if (maxSeverity.equals(FacesMessage.SEVERITY_ERROR)) {
                return " alert-danger";
            } else if (maxSeverity.equals(FacesMessage.SEVERITY_WARN)) {
                return " alert-warning";
            } else if (maxSeverity.equals(FacesMessage.SEVERITY_INFO)) {
                return " alert-success";
            } else {
                return " alert-primary";
            }
        }
        return "";
    }

    /**
     * Retrieves and returns the URL to redirect to after login.
     *
     * @return The URL to redirect to after login.
     */
    public String getRedirectUrl() {
        ExternalContext ectx = fctx.getExternalContext();

        String base = ectx.getApplicationContextPath();
        HttpServletRequest request = (HttpServletRequest) ectx.getRequest();
        UrlMapping mapping = PrettyContext.getCurrentInstance().getCurrentMapping();
        String uri = mapping != null ? mapping.getPattern() : request.getRequestURI();
        String queryString = request.getQueryString();

        return URLEncoder.encode(base + uri + (queryString == null ? "" : '?' + queryString), StandardCharsets.UTF_8);
    }

    /**
     * Returns the current year in the gregorian calendar.
     *
     * @return The current year as integer.
     */
    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * @return {@code true} if the Menu should be displayed, {@code false} otherwise.
     */
    public boolean isDisplayMenu() {
        return displayMenu;
    }

    private void closeMenu() {
        displayMenu = false;
    }

    private void openMenu() {
        displayMenu = true;
    }

    /**
     * Enables suggestions for users to be unbanned.
     */
    public void updateSuggestions() {
        if (search != null && !search.isBlank()) {
            userSearchSuggestion = searchService.getUserSuggestions(search);
            topicSearchSuggestion = searchService.getTopicSuggestions(search);
            reportSearchSuggestion = searchService.getReportSuggestions(search);
        }
    }

    /**
     * @param search The new search query.
     */
    public void setSearch(final String search) {
        this.search = search;
    }

    /**
     * @return The current search query
     */
    public String getSearch() {
        return search;
    }

    /**
     * @return The userSearchSuggestion.
     */
    public List<String> getUserSearchSuggestion() {
        return userSearchSuggestion;
    }

    /**
     * @param userSearchSuggestion The userSearchSuggestion to set.
     */
    public void setUserSearchSuggestion(final List<String> userSearchSuggestion) {
        this.userSearchSuggestion = userSearchSuggestion;
    }

    /**
     * @return The topicSearchSuggestion.
     */
    public List<String> getTopicSearchSuggestion() {
        return topicSearchSuggestion;
    }

    /**
     * @param topicSearchSuggestion The topicSearchSuggestion to set.
     */
    public void setTopicSearchSuggestion(final List<String> topicSearchSuggestion) {
        this.topicSearchSuggestion = topicSearchSuggestion;
    }

    /**
     * @return The reportSearchSuggestion.
     */
    public List<String> getReportSearchSuggestion() {
        return reportSearchSuggestion;
    }

    /**
     * @param reportSearchSuggestion The reportSearchSuggestion to set.
     */
    public void setReportSearchSuggestion(final List<String> reportSearchSuggestion) {
        this.reportSearchSuggestion = reportSearchSuggestion;
    }


}
