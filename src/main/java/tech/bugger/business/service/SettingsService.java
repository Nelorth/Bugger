package tech.bugger.business.service;

import tech.bugger.business.util.Feedback;
import tech.bugger.business.util.RegistryKey;
import tech.bugger.global.transfer.Configuration;
import tech.bugger.global.transfer.Organization;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.NotFoundException;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * Service for application settings.
 */
@Dependent
public class SettingsService {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(SettingsService.class);

    /**
     * Transaction manager used for creating transactions.
     */
    private final TransactionManager transactionManager;

    /**
     * Feedback Event for user feedback.
     */
    private final Event<Feedback> feedbackEvent;

    /**
     * Resource bundle for feedback messages.
     */
    private final ResourceBundle messagesBundle;

    /**
     * Constructs a new settings service with the given dependencies.
     *
     * @param transactionManager The transaction manager to use for creating transactions.
     * @param feedbackEvent      The feedback event to use for user feedback.
     * @param messagesBundle     The resource bundle for feedback messages.
     */
    @Inject
    public SettingsService(final TransactionManager transactionManager, final Event<Feedback> feedbackEvent,
                           final @RegistryKey("messages") ResourceBundle messagesBundle) {
        this.transactionManager = transactionManager;
        this.feedbackEvent = feedbackEvent;
        this.messagesBundle = messagesBundle;
    }

    /**
     * Loads the current application configuration.
     *
     * @return The loaded {@link Configuration}.
     */
    public Configuration loadConfiguration() {
        Configuration configuration = null;
        try (Transaction tx = transactionManager.begin()) {
            configuration = tx.newSettingsGateway().getConfiguration();
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Application configuration not found.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when loading application configuration.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return configuration;
    }

    /**
     * Loads the current organization data.
     *
     * @return The loaded {@link Organization}.
     */
    public Organization loadOrganization() {
        Organization organization = null;
        try (Transaction tx = transactionManager.begin()) {
            organization = tx.newSettingsGateway().getOrganization();
            tx.commit();
        } catch (NotFoundException e) {
            log.error("Organization data not found.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("not_found_error"), Feedback.Type.ERROR));
        } catch (TransactionException e) {
            log.error("Error when loading organization data.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("data_access_error"), Feedback.Type.ERROR));
        }
        return organization;
    }

    /**
     * Updates the application configuration in the data storage.
     *
     * @param configuration The new {@link Configuration}.
     * @return {@code true} iff the update succeeded.
     */
    public boolean updateConfiguration(final Configuration configuration) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newSettingsGateway().setConfiguration(configuration);
            tx.commit();
            feedbackEvent.fire(new Feedback(messagesBundle.getString("operation_successful"), Feedback.Type.INFO));
            return true;
        } catch (TransactionException e) {
            feedbackEvent.fire(new Feedback(messagesBundle.getString("update_failure"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Updates the organization data in the data storage.
     *
     * @param organization The new {@link Organization}.
     * @return {@code true} iff the update succeeded.
     */
    public boolean updateOrganization(final Organization organization) {
        try (Transaction tx = transactionManager.begin()) {
            tx.newSettingsGateway().setOrganization(organization);
            tx.commit();
            feedbackEvent.fire(new Feedback(messagesBundle.getString("operation_successful"), Feedback.Type.INFO));
            return true;
        } catch (TransactionException e) {
            feedbackEvent.fire(new Feedback(messagesBundle.getString("update_failure"), Feedback.Type.ERROR));
            return false;
        }
    }

    /**
     * Discovers all regular files non-recursively in the given directory.
     *
     * @param path The path of the directory to discover files in.
     * @return The filenames of the files in {@code path}.
     */
    public List<String> discoverFiles(final String path) {
        List<String> filenames = new ArrayList<>();
        try (Stream<Path> files = Files.list(Paths.get(path))) {
            files.filter(Files::isRegularFile)
                 .map(p -> p.getFileName().toString())
                 .forEach(filenames::add);
        } catch (IOException e) {
            log.error("Could not discover available themes on file system.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("themes_discovery_error"), Feedback.Type.WARNING));
        }
        return filenames;
    }

    /**
     * Reads the given input stream to the internal format for further use.
     *
     * @param is The uploaded file as input stream.
     * @return The fully read file as {@code byte[]} array or {@code null} iff reading failed.
     */
    public byte[] readFile(final InputStream is) {
        byte[] bytes = null;
        try {
            bytes = is.readAllBytes();
        } catch (IOException e) {
            log.warning("Could not read uploaded file.", e);
            feedbackEvent.fire(new Feedback(messagesBundle.getString("logo_conversion_error"), Feedback.Type.ERROR));
        }
        return bytes;
    }

}
