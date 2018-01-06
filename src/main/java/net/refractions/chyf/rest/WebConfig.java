package net.refractions.chyf.rest;

import java.nio.charset.Charset;
import java.util.List;

import net.refractions.chyf.rest.messageconverters.HtmlResponseConverter;
import net.refractions.chyf.rest.messageconverters.JsonErrorMessageConverter;
import net.refractions.chyf.rest.messageconverters.JsonResponseConverter;
import net.refractions.chyf.rest.messageconverters.JsonpResponseConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan("net.refractions.chyf.rest")
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
		
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}
	
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(jsonErrorMessageConverter());
		converters.add(jsonResponseConverter());
		converters.add(jsonpResponseConverter());
		converters.add(htmlResponseConverter());
		super.configureMessageConverters(converters);
	}	
	
	@Bean
	public JsonErrorMessageConverter jsonErrorMessageConverter() {
		return new JsonErrorMessageConverter();
	}
	
	@Bean
	public JsonResponseConverter jsonResponseConverter() {
		return new JsonResponseConverter();
	}

	@Bean
	public JsonpResponseConverter jsonpResponseConverter() {
		return new JsonpResponseConverter();
	}

	@Bean
	public HtmlResponseConverter htmlResponseConverter() {
		return new HtmlResponseConverter();
	}

//	@Override
//	public void addFormatters(FormatterRegistry registry) {
//	}
	
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer
				.favorPathExtension(true)
				.favorParameter(false)
				.ignoreAcceptHeader(true)
				.useJaf(false)
				.defaultContentType(MediaType.APPLICATION_XHTML_XML)
				.mediaType("xhtml", MediaType.APPLICATION_XHTML_XML)
				.mediaType("html", MediaType.TEXT_HTML)
				.mediaType("json", MediaType.APPLICATION_JSON)
				.mediaType("geojson", new MediaType("application", "vnd.geo+json",
						Charset.forName("UTF-8")))
				.mediaType("jsonp",
						new MediaType("application", "javascript", Charset.forName("UTF-8")))
				.mediaType("geojsonp",
						new MediaType("application", "javascript", Charset.forName("UTF-8")));
	}
	
}
