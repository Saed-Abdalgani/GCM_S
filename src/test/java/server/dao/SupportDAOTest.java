package server.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SupportDAO.
 * Verifies FAQ matching and basic DAO structure.
 */
public class SupportDAOTest {

    @BeforeEach
    public void setUp() throws Exception {
        // Setup not required for static methods test or assume DB is ready
    }

    @Test
    @DisplayName("Find Matching FAQ returns answer for strong match")
    public void testFindMatchingFaq_StrongMatch() {
        // "purchase" and "map" appear in seeded FAQ
        String answer = SupportDAO.findMatchingFaq("How do I purchase a map?");
        assertNotNull(answer, "Should find an answer");
        assertTrue(answer.contains("Browse Catalog"), "Answer should contain purchase instructions");
    }

    @Test
    @DisplayName("Find Matching FAQ returns null for no match")
    public void testFindMatchingFaq_NoMatch() {
        String answer = SupportDAO.findMatchingFaq("Something confusing unrelated xyz");
        assertNull(answer, "Should not find an answer");
    }

    @Test
    @DisplayName("SupportDAO class exists and is loadable")
    public void testDAOStructure() {
        // Compile check
        try {
            Class.forName("server.dao.SupportDAO");
        } catch (ClassNotFoundException e) {
            fail("SupportDAO class should exist");
        }
    }
}
