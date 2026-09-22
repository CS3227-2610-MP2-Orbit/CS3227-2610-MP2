package seedu.eventmanager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/** Applies the idempotent schema required by the current application. */
public final class DatabaseMigration {
    private static final String MIGRATION_RESOURCE =
            "/db/migration/V1__create_organizer_events.sql";

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() throws SQLException {
        String migration = loadMigration();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static String loadMigration() {
        try (InputStream stream = DatabaseMigration.class.getResourceAsStream(MIGRATION_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing database migration " + MIGRATION_RESOURCE);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read database migration", exception);
        }
    }
}
