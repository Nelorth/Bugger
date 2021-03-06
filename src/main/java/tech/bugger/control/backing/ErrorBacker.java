package tech.bugger.control.backing;

import tech.bugger.business.internal.ApplicationSettings;
import tech.bugger.business.util.MarkdownHandler;
import tech.bugger.control.util.JFConfig;
import tech.bugger.global.util.Log;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Backing Bean for the error page.
 */
@RequestScoped
@Named
public class ErrorBacker {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(ErrorBacker.class);

    /**
     * The current application settings.
     */
    private final ApplicationSettings applicationSettings;

    /**
     * Constructs a new error page backing bean with the necessary dependencies.
     *
     * @param applicationSettings The application settings to use.
     */
    @Inject
    public ErrorBacker(final ApplicationSettings applicationSettings) {
        this.applicationSettings = applicationSettings;
    }

    /**
     * Initializes the error page.
     */
    @PostConstruct
    void init() {
        log.debug("init called on error backer.");
    }

    /**
     * Parses and returns the support information in HTML.
     *
     * @return The parsed support information in HTML.
     */
    public String getSupportInformation() {
        String support = applicationSettings.getOrganization().getSupportInfo();
        return support == null ? "" : MarkdownHandler.toHtml(support);
    }

    /**
     * Returns the stack trace of the given exception as a String.
     *
     * @param exception The given exception.
     * @return The stack trace as a String.
     */
    public static String stackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        if (exception != null) {
            exception.printStackTrace(printWriter);
        }

        return stringWriter.toString();
    }

    /**
     * Returns a link leading to the home page.
     *
     * @return The link leading to the home page.
     */
    public static String getHomeUrl() {
        return JFConfig.getApplicationPath(FacesContext.getCurrentInstance().getExternalContext()) + "/";
    }

}
