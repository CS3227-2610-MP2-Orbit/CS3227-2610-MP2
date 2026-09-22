package seedu.eventmanager.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;

/** Provides PostgreSQL connections and a transaction-scoped connection. */
public final class JdbcDatabase {
    private final DatabaseConfiguration configuration;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public JdbcDatabase(DatabaseConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    public <T> T withConnection(Function<Connection, T> operation) {
        Connection active = transactionConnection.get();
        if (active != null) {
            return operation.apply(active);
        }
        try (Connection connection = openConnection()) {
            return operation.apply(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not connect to PostgreSQL.", exception);
        }
    }

    public <T> T inTransaction(Function<Connection, T> operation) {
        if (transactionConnection.get() != null) {
            return operation.apply(transactionConnection.get());
        }
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                T result = operation.apply(connection);
                connection.commit();
                return result;
            } catch (RuntimeException | Error exception) {
                connection.rollback();
                throw exception;
            } finally {
                transactionConnection.remove();
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("The PostgreSQL transaction failed.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(configuration.url(), configuration.username(), configuration.password());
    }
}
