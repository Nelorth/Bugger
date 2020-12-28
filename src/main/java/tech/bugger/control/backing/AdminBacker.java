package tech.bugger.control.backing;

import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.service.SettingsService;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.Organization;
import tech.bugger.global.util.Log;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Part;
import java.util.List;

/**
 * Backing Bean for the admin page.
 */
@RequestScoped
@Named
public class AdminBacker {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(AdminBacker.class);

    /**
     * Temporary configuration until submit.
     */
    private Configuration configuration;

    /**
     * Temporary organization until submit.
     */
    private Organization organization;

    /**
     * The application settings cache.
     */
    private ApplicationSettings applicationSettings;

    /**
     * The settings service providing logic.
     */
    private SettingsService settingsService;

    /**
     * Reference to the current {@link FacesContext}.
     */
    private FacesContext fctx;

    /**
     * Constructs a new admin page backing bean with the necessary dependencies.
     *
     * @param applicationSettings The application settings cache.
     * @param settingsService     The settings service to use.
     * @param fctx                The current {@link FacesContext} of the application.
     */
    @Inject
    public AdminBacker(final ApplicationSettings applicationSettings, final SettingsService settingsService,
                       final FacesContext fctx) {
        this.applicationSettings = applicationSettings;
        this.settingsService = settingsService;
        this.fctx = fctx;
    }

    /**
     * Initializes the admin page. Checks if the user is allowed to access the page. If not, acts as if the page did not
     * exist. Puts the names of the available CSS files into {@code availableThemes}.
     */
    @PostConstruct
    public void init() {
        configuration = new Configuration(applicationSettings.getConfiguration());
        organization = new Organization(applicationSettings.getOrganization());
    }

    /**
     * Redirects to the place where to browse users.
     *
     * @return The direction.
     */
    public String browseUsers() {
        // TODO redirect to search page on users tab with no filters
        System.out.println("braus");
        return null;
    }

    /**
     * Redirects to the place where to create a new user.
     *
     * @return The direction.
     */
    public String createUser() {
        // TODO redirect to edit-user.xhtml with parameters
        System.out.println("kriäit");
        return null;
    }

    /**
     * Converts the uploaded logo into a {@code byte[]} and puts it into the temporary configuration.
     *
     * This method is only called when the uploaded file actually changes.
     *
     * @param vce The event fired upon change in uploaded file.
     */
    public void uploadLogo(final ValueChangeEvent vce) {
        Part upload = (Part) vce.getNewValue();
        byte[] logo = settingsService.convertLogo(upload);
        if (logo != null) {
            organization.setLogo(logo);
        }
    }

    /**
     * Saves and applies the changes made to the application configuration.
     */
    public void saveConfiguration() {
        if (settingsService.updateConfiguration(configuration)) {
            applicationSettings.setConfiguration(configuration);
        }
    }

    /**
     * Saves and applies the changes made to the organization data.
     */
    public void saveOrganization() {
        if (settingsService.updateOrganization(organization)) {
            applicationSettings.setOrganization(organization);
        }
    }

    /**
     * {@code availableThemes} holds information about which themes can be selected.
     *
     * @return The availableThemes.
     */
    public List<String> getAvailableThemes() {
        return settingsService.discoverThemes();
    }

    /**
     * {@code configuration} holds information about system-wide moderation options without those options already being
     * in effect.
     *
     * @return The configuration.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@code configuration} holds information about system-wide moderation options without those options already being
     * in effect.
     *
     * @param configuration The configuration to set.
     */
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * {@code organization} holds details for modifying things like the appearance of the front end without those
     * options being already effective.
     *
     * @return The organization.
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * {@code organization} holds details for modifying things like the appearance of the front end without those
     * options being already effective.
     *
     * @param organization The organization to set.
     */
    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

}
