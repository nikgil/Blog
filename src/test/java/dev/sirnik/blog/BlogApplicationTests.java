package dev.sirnik.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogApplicationTests {

	private final MockMvc mockMvc;

	@Autowired
	BlogApplicationTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void contextLoads() {
	}

	@Test
	void homePageLoads() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Hello World")));
	}

	@Test
	void testHomeRendersPostPreviews() throws Exception {
		mockMvc.perform(get("/test-home").param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("post-preview__tag-scroll")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("post-preview__body")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Lorem ipsum")));
	}

}
