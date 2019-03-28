package com.mediatoolkit.pareco.web;

import freemarker.cache.FileTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-24
 */
@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/static/**")
			.addResourceLocations(
				"file:parecoclientrunner/src/main/resources/web-ui/static/",
				"classpath:/web-ui/static/"
			);
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addRedirectViewController("/", "/transfers");
	}

	@Bean
	public FreeMarkerViewResolver freemarkerViewResolver() {
		FreeMarkerViewResolver freeMarkerViewResolver = new FreeMarkerViewResolver();
		freeMarkerViewResolver.setCache(false);
		freeMarkerViewResolver.setSuffix(".ftl");
		return freeMarkerViewResolver;
	}

	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer() throws IOException, TemplateException {
		FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();
		Properties properties = new Properties();
		properties.setProperty("template_update_delay", "0");
		freeMarkerConfigurer.setFreemarkerSettings(properties);
		freeMarkerConfigurer.setTemplateLoaderPaths(
			"file:parecoclientrunner/src/main/resources/web-ui",    //for faster development
			"classpath:/web-ui/"
		);
		freeMarkerConfigurer.afterPropertiesSet();
		freeMarkerConfigurer.getConfiguration().setOutputFormat(HTMLOutputFormat.INSTANCE);
		return freeMarkerConfigurer;
	}




}

