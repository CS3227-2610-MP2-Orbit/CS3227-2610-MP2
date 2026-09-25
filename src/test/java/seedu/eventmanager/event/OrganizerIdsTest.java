package seedu.eventmanager.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizerIdsTest {
    @Test
    void toUuid_parsesUuidShapedUserId() {
        UUID expected = UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertEquals(expected, OrganizerIds.toUuid(expected.toString()));
    }

    @Test
    void toUuid_nonUuidUsesStableNameUuid() {
        UUID expected = UUID.nameUUIDFromBytes("organizer:demo-organizer".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, OrganizerIds.toUuid("demo-organizer"));
        assertNotEquals(expected, OrganizerIds.toUuid("other-organizer"));
    }

    @Test
    void toUuid_blank_rejected() {
        assertThrows(IllegalArgumentException.class, () -> OrganizerIds.toUuid("  "));
    }
}
