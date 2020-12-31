package tech.bugger.persistence.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tech.bugger.DBExtension;
import tech.bugger.LogExtension;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LogExtension.class)
@ExtendWith(DBExtension.class)
class TopicDBGatewayTest {

    private TopicDBGateway gateway;
    private Connection connection;

    @BeforeEach
    public void setUp() throws Exception {
        connection = DBExtension.getConnection();
        gateway = new TopicDBGateway(connection);
    }

    @AfterEach
    public void tearDown() throws Exception {
        connection.close();
    }

    private void addTopics() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DO\n" +
                    "$$\n" +
                    "BEGIN\n" +
                    "FOR i IN 1..3 LOOP\n" +
                    "    INSERT INTO topic (title, description) VALUES\n" +
                    "        (CONCAT('testtopic', CURRVAL('topic_id_seq') + 1), CONCAT('Description for testtopic', CURRVAL('topic_id_seq') + 1));\n" +
                    "END LOOP;\n" +
                    "END;\n" +
                    "$$\n" +
                    ";\n");
        }
    }

    @Test
    public void getNumberOfTopicsWhenThereAreSome() throws Exception {
        addTopics();
        assertEquals(3, gateway.getNumberOfTopics());
    }

    @Test
    public void getNumberOfTopicsWhenThereAreNone() {
        assertEquals(0, gateway.getNumberOfTopics());
    }
}