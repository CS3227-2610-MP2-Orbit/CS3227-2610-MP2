package seedu.eventmanager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/** Applies the idempotent schema required by the current application. */
public final class DatabaseMigration {
    private static final List<String> MIGRATION_RESOURCES = List.of(
            "/db/organizer/V1__create_organizer_events.sql",
            "/db/organizer/V2__create_event_volunteers.sql");

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String resource : MIGRATION_RESOURCES) {
                for (String sql : loadMigration(resource).split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
        }
    }

    private static String loadMigration(String resource) {
        try (InputStream stream = DatabaseMigration.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing database migration " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read database migration", exception);
        }
    }
}
