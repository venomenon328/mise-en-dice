package io.github.venomenon328.miseendice;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AdministrationSecurityIntegrationTest extends CurrentSchemaPostgresIntegrationTest {

    private static final String ACTOR_KEY = "security-test-admin";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("mise-en-dice.administration.enabled", () -> true);
        registry.add("mise-en-dice.administration.accounts[0].actor-key", () -> ACTOR_KEY);
        registry.add("mise-en-dice.administration.accounts[0].display-name", () -> "Security Test Admin");
        registry.add("mise-en-dice.administration.accounts[0].password-hash", () -> PASSWORD_HASH);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedAdministrationRequestsAreRedirectedToTheFormLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unauthenticatedAdministrationRequestsWithTrailingSlashAreRedirectedToTheFormLogin() throws Exception {
        mockMvc.perform(get("/admin/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void correctlyAuthenticatedAdministrationRequestsPassTheSecurityLayer() throws Exception {
        mockMvc.perform(get("/admin").session(authenticate()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/catalog"));
    }

    @Test
    void correctlyAuthenticatedTrailingSlashAdministrationRequestsRedirectToTheCatalog() throws Exception {
        mockMvc.perform(get("/admin/").session(authenticate()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/catalog"));
    }

    @Test
    void csrfProtectionRejectsAuthenticatedWritesWithoutAToken() throws Exception {
        mockMvc.perform(post("/admin").session(authenticate()))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession authenticate() throws Exception {
        MvcResult login = mockMvc.perform(formLogin().user(ACTOR_KEY).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withUsername(ACTOR_KEY))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
