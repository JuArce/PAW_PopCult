package ar.edu.itba.paw.webapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Properties;

@EnableAsync
@ComponentScan({"ar.edu.itba.paw.services"})
@Configuration
@PropertySource({"classpath:/config/mail-config-develop.properties"})
//@PropertySource({ "classpath:/config/mail-config-production.properties" })
public class EmailConfig {

    @Autowired
    private Environment environment;

    @Autowired
    private MessageSource messageSource;

    @Bean
    public JavaMailSender javaMailSender() {
        final JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(environment.getProperty("mail.host"));
        javaMailSender.setPort(Integer.parseInt(environment.getProperty("mail.port")));

        javaMailSender.setUsername(environment.getProperty("mail.username"));
        javaMailSender.setPassword(environment.getProperty("mail.password"));

        Properties properties = javaMailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", environment.getProperty("mail.transport.protocol"));
        properties.put("mail.smtp.auth", environment.getProperty("mail.smtp.auth"));
        properties.put("mail.smtp.starttls.enable", environment.getProperty("mail.smtp.starttls.enable"));
        properties.put("mail.debug", environment.getProperty("mail.debug"));

        return javaMailSender;
    }

    @Bean
    public ITemplateResolver thymeleafTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();

        templateResolver.setPrefix("emailTemplates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");

        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine springtemplateEngine(ITemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(messageSource);

        return templateEngine;
    }
}
