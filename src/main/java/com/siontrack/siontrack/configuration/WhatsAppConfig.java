package com.siontrack.siontrack.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppConfig {

    @Getter @Setter
    private String token_acceso;
    @Getter @Setter
    private String ID_numero;
    @Getter @Setter
    private String apiVersion ;
    @Getter @Setter
    private String codigoPais;
    @Getter @Setter
    private Webhook webhook = new Webhook();
    @Getter @Setter
    private Plantillas plantillas = new Plantillas();

    public String getApiUrl() {
        return String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, ID_numero);
    }

    public static class Webhook {
        @Getter @Setter
        private String tokenverificacion;
    }

    public static class Plantillas {
        @Getter @Setter
        private String consentimiento ;
        @Getter @Setter
        private String recordatorio;
        @Getter @Setter
        private String promocion;
        @Getter @Setter
        private String idioma = "es";
    }

}
