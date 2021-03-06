package tech.bugger.business.internal;

import java.time.Duration;
import tech.bugger.global.util.Log;
import tech.bugger.persistence.exception.TransactionException;
import tech.bugger.persistence.util.Transaction;
import tech.bugger.persistence.util.TransactionManager;

/**
 * Periodically running task for maintenance purposes.
 */
public class PeriodicCleaner implements Runnable {

    /**
     * The {@link Log} instance associated with this class for logging purposes.
     */
    private static final Log log = Log.forClass(PeriodicCleaner.class);

    /**
     * The time after which data in the data source is considered expired.
     */
    private static final Duration EXPIRATION_AGE = Duration.ofHours(1);

    /**
     * The transaction manager used for creating transactions.
     */
    private final TransactionManager transactionManager;

    /**
     * Creates a new periodic data source cleaner with the given dependencies.
     *
     * @param transactionManager The transaction manager to be used for creating transactions.
     */
    public PeriodicCleaner(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Clean up data not needed any more.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void run() {
        log.info("Periodic cleaner started.");
        try (Transaction tx = transactionManager.begin()) {
            tx.newTokenGateway().cleanExpiredTokens(EXPIRATION_AGE);
            tx.newUserGateway().cleanExpiredRegistrations();
            tx.commit();
            log.debug("Finished cleaning data source successfully.");
        } catch (TransactionException e) {
            log.error("Transaction commit error when cleaning data source.", e);
        }
        log.info("Periodic cleaner finished.");
    }

}
