package com.khanh.newsaggregator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"news.ingestion.enabled=false",
		"news.trending.enabled=false"
})
class NewsAggregatorApplicationTests {

	@Test
	void contextLoads() {
	}

}
