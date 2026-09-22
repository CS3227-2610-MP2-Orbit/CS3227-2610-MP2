package seedu.eventmanager.storage;

import java.util.Objects;
import java.util.function.Supplier;
import seedu.eventmanager.service.TransactionManager;

/** Adapts the JDBC transaction boundary to the application service contract. */
public final class JdbcTransactionManager implements TransactionManager {
    private final JdbcDatabase database;

    public JdbcTransactionManager(JdbcDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        Objects.requireNonNull(work);
        return database.inTransaction(connection -> work.get());
    }
}
