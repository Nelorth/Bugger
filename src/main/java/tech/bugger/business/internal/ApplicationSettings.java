package tech.bugger.business.internal;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import tech.bugger.business.service.SettingsService;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.Organization;

/**
 * Cache of application-wide settings.
 */
@ApplicationScoped
@Named
public class ApplicationSettings {

    /**
     * The cached application configuration.
     */
    private Configuration configuration;

    /**
     * The cached organization data.
     */
    private Organization organization;

    /**
     * Settings service providing logic.
     */
    private final SettingsService settingsService;

    /**
     * Constructs a new application settings cache with the necessary dependencies.
     *
     * @param settingsService The settings service to use.
     */
    @Inject
    public ApplicationSettings(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Loads the application settings from the data store.
     */
    @PostConstruct
    public void loadSettings() {
        configuration = settingsService.loadConfiguration();
        organization = settingsService.loadOrganization();
    }

    /**
     * Gets the configuration.
     *
     * @return The configuration.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration.
     *
     * @param configuration The configuration to set.
     */
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the organization settings.
     *
     * @return The organization settings.
     */
    public Organization getOrganization() {
        return organization;
    }

    /**
     * Sets the organization settings.
     *
     * @param organization The organization settings to set.
     */
    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

}
