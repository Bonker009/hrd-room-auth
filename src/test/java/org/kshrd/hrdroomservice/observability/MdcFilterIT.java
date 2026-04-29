package org.kshrd.hrdroomservice.observability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.kshrd.hrdroomservice.support.IntegrationTest;

class MdcFilterIT extends IntegrationTest {

    @Test
    void generatesRequestIdHeaderWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v4/academic-years/active"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(RequestContextFilter.REQUEST_ID_HEADER));
    }

    @Test
    void preservesClientProvidedRequestId() throws Exception {
        String requestId = "req-custom-123";
        mockMvc.perform(get("/api/v4/academic-years/active").header("X-Request-Id", requestId))
                .andExpect(status().isNotFound())
                .andExpect(header().string(RequestContextFilter.REQUEST_ID_HEADER, requestId));
    }
}
