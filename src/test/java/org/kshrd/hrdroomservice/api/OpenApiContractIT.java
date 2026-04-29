package org.kshrd.hrdroomservice.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "app.security.enabled=false",
            "spring.flyway.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:openapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@AutoConfigureMockMvc
class OpenApiContractIT {

    private static final Path SNAPSHOT_PATH =
            Path.of("src", "test", "resources", "openapi", "openapi.json");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void openApiContractMatchesSnapshot() throws Exception {
        String body =
                mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode live = normalize(objectMapper.readTree(body));
        boolean updateMode = Boolean.parseBoolean(System.getProperty("openapi.update", "false"));

        if (updateMode || Files.notExists(SNAPSHOT_PATH)) {
            writeSnapshot(live);
            if (!updateMode) {
                Assertions.fail(
                        "OpenAPI snapshot was missing and has now been created. "
                                + "Re-run tests and commit src/test/resources/openapi/openapi.json");
            }
            return;
        }

        JsonNode snapshot = normalize(objectMapper.readTree(Files.readString(SNAPSHOT_PATH)));
        Assertions.assertEquals(
                snapshot,
                live,
                "OpenAPI contract drift detected. "
                        + "If intended, run: ./gradlew test --tests OpenApiContractIT -Dopenapi.update=true "
                        + "and commit the updated snapshot.");
    }

    private JsonNode normalize(JsonNode node) {
        JsonNode copy = node.deepCopy();
        if (copy instanceof ObjectNode objectNode) {
            objectNode.remove("servers");
            JsonNode info = objectNode.get("info");
            if (info instanceof ObjectNode infoNode) {
                infoNode.remove("version");
            }
        }
        return copy;
    }

    private void writeSnapshot(JsonNode node) throws IOException {
        Files.createDirectories(SNAPSHOT_PATH.getParent());
        ObjectMapper mapper = objectMapper.copy();
        Files.writeString(
                SNAPSHOT_PATH, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }
}
