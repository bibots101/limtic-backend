package tn.limtic.limtic_backend.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import tn.limtic.limtic_backend.model.ParametreSysteme;
import tn.limtic.limtic_backend.repository.ParametreSystemeRepository;

@Service
public class SmtpSettingsService {

    private final ParametreSystemeRepository repo;

    @Value("${spring.mail.host:}")
    private String envHost;

    @Value("${spring.mail.port:0}")
    private int envPort;

    @Value("${spring.mail.username:}")
    private String envUsername;

    @Value("${spring.mail.password:}")
    private String envPassword;

    public SmtpSettingsService(ParametreSystemeRepository repo) {
        this.repo = repo;
    }

    public JavaMailSender createMailSender() {
        SmtpSettings settings = getSettings();
        if (settings.host().isBlank() || settings.username().isBlank() || settings.password().isBlank()) {
            throw new IllegalStateException("SMTP settings are missing. Configure DB or environment.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setUsername(settings.username());
        sender.setPassword(settings.password());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return sender;
    }

    public String getDestinataire() {
        String dbValue = getValue("smtp.destinataire");
        if (!dbValue.isBlank()) return dbValue;
        return envUsername == null ? "" : envUsername.trim();
    }

    public SmtpSettings getSettings() {
        String host = pick(getValue("smtp.host"), envHost);
        int port = parsePort(pick(getValue("smtp.port"), envPort > 0 ? String.valueOf(envPort) : ""), envPort);
        String username = pick(getValue("smtp.username"), envUsername);
        String password = pick(getValue("smtp.password"), envPassword);

        return new SmtpSettings(host, port, username, password);
    }

    private String getValue(String key) {
        return repo.findByCle(key).map(ParametreSysteme::getValeur).orElse("");
    }

    private String pick(String value, String fallback) {
        if (value != null && !value.isBlank()) return value.trim();
        return fallback == null ? "" : fallback.trim();
    }

    private int parsePort(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public record SmtpSettings(String host, int port, String username, String password) {}
}
