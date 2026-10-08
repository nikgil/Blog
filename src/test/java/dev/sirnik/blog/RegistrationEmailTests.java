package dev.sirnik.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import dev.sirnik.blog.models.AdminUser;
import dev.sirnik.blog.repositories.AdminUserRepository;
import dev.sirnik.blog.utils.JsonWebTokenUtils;
import dev.sirnik.blog.utils.JsonWebTokenUtils.JWTPayload;

/**
 * The approval email, end to end through {@code POST /register}. JavaMailSender
 * is replaced with a mock, so no SMTP server is involved. The send is
 * {@code @Async}, which is why the verifications use {@code timeout(...)} to
 * wait for the background thread and {@code after(...).never()} to prove that
 * nothing is sent. {@code @Transactional} rolls the rows back; the async thread
 * only reads the entity it was handed, so it needs no transaction of its own.
 */
@SpringBootTest(properties = {"blog.registration.enabled=true",
    "blog.registration.jwt=0123456789abcdef0123456789abcdef",
    "blog.registration.url=https://blog.example.com",
    "blog.registration.mail.approver=owner@example.com",
    "blog.registration.mail.sender=blog@example.com",
    // aMailServerOutageDoesNotBreakRegistration throws on purpose, and @Async
    // reports that through this logger as an ERROR with a stack trace.
    "logging.level.org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler=OFF"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationEmailTests {

    private static final String JWT_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String LINK_PREFIX = "https://blog.example.com/register/confirm?confirmation=";
    private static final Pattern LINK = Pattern
        .compile("confirmation=([^\\s]+)");

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    void registrationSendsOneApprovalEmailToTheOwner() throws Exception {
        saveActivatedOwner();

        register("ada", "ada@example.com");

        SimpleMailMessage message = sentMessage();
        assertThat(message.getTo()).containsExactly("owner@example.com");
        assertThat(message.getFrom()).isEqualTo("blog@example.com");
        assertThat(message.getSubject()).contains("ada");
        assertThat(message.getText())
            .contains("ada")
            .contains("ada@example.com")
            .contains(LINK_PREFIX);
    }

    @Test
    void theEmailedTokenIdentifiesTheNewUserAndIsStillValid() throws Exception {
        saveActivatedOwner();

        register("ada", "ada@example.com");

        AdminUser saved = adminUserRepository
            .findByUsername("ada")
            .orElseThrow();
        JWTPayload payload = JsonWebTokenUtils
            .parsePayload(JWT_SECRET, tokenFrom(sentMessage()));

        assertThat(payload)
            .isEqualTo(
                new JWTPayload(
                    "ada",
                    "ada@example.com",
                    saved.getCreatedAt().toEpochMilli()
                )
            );
        assertThat(
            JsonWebTokenUtils
                .verifyPayload(
                    payload, adminUserRepository, System.currentTimeMillis()
                )
        ).isTrue();
    }

    @Test
    void theOwnerCanApproveTheUserWithTheLinkFromTheEmail() throws Exception {
        saveActivatedOwner();
        register("ada", "ada@example.com");
        String token = tokenFrom(sentMessage());

        mockMvc
            .perform(
                get("/register/confirm")
                    .param("confirmation", token)
                    .with(user("owner").roles("ADMIN"))
            )
            .andExpect(status().isOk())
            .andExpect(view().name("verify_success"))
            .andExpect(model().attribute("username", "ada"));

        assertThat(
            adminUserRepository
                .findByUsername("ada")
                .orElseThrow()
                .isActivated()
        ).isTrue();
    }

    @Test
    void theVeryFirstUserIsAutoApprovedAndNoEmailIsSent() throws Exception {
        // Empty table: saveUser activates the first account, so there is
        // nothing for the owner to approve.
        register("first", "first@example.com");

        assertThat(
            adminUserRepository
                .findByUsername("first")
                .orElseThrow()
                .isActivated()
        ).isTrue();
        verify(mailSender, after(500).never())
            .send(any(SimpleMailMessage.class));
    }

    @Test
    void aRejectedRegistrationSendsNoEmail() throws Exception {
        saveActivatedOwner();

        // "owner" is taken, so the form is rejected.
        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("name", "owner")
                    .param("email", "other@example.com")
                    .param("password", "password")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("register"));

        verify(mailSender, after(500).never())
            .send(any(SimpleMailMessage.class));
    }

    @Test
    void theEmailIsSentOnAnotherThreadThanTheRequest() throws Exception {
        saveActivatedOwner();
        AtomicReference<Thread> senderThread = new AtomicReference<>();
        doAnswer(invocation -> {
            senderThread.set(Thread.currentThread());
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        // MockMvc handles the request on this very thread, so if @Async were
        // missing (or the method were called from inside its own class), the
        // send would happen right here.
        register("ada", "ada@example.com");

        verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        assertThat(senderThread.get())
            .isNotNull()
            .isNotSameAs(Thread.currentThread());
    }

    @Test
    void aMailServerOutageDoesNotBreakRegistration() throws Exception {
        saveActivatedOwner();
        doThrow(new MailSendException("smtp is down"))
            .when(mailSender)
            .send(any(SimpleMailMessage.class));

        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("name", "ada")
                    .param("email", "ada@example.com")
                    .param("password", "password")
            )
            .andExpect(status().isOk())
            .andExpect(model().attribute("registered", true));

        verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        assertThat(adminUserRepository.findByUsername("ada")).isPresent();
    }

    private void register(
        String name,
        String email
    ) throws Exception {
        mockMvc
            .perform(
                post("/register")
                    .with(csrf())
                    .param("name", name)
                    .param("email", email)
                    .param("password", "password")
            )
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attribute("registered", true));
    }

    private void saveActivatedOwner() {
        AdminUser owner = new AdminUser(
            "owner-login@example.com",
            "owner",
            "{noop}unused-hash"
        );
        owner.setActivated(true);
        adminUserRepository.saveAndFlush(owner);
    }

    /** Waits for the background send, then returns what was handed over. */
    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor
            .forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000)).send(captor.capture());
        return captor.getValue();
    }

    private static String tokenFrom(SimpleMailMessage message) {
        Matcher matcher = LINK.matcher(message.getText());
        assertThat(matcher.find()).as("link in email body").isTrue();
        return matcher.group(1);
    }
}
