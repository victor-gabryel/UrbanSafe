package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

    private SimpleMailMessage enviar(String urlFrontend, long horas) {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "remetente", "no-reply@exemplo.com");
        ReflectionTestUtils.setField(service, "urlFrontend", urlFrontend);
        ReflectionTestUtils.setField(service, "expiracaoVerificacaoHoras", horas);
        ReflectionTestUtils.setField(service, "expiracaoRecuperacaoMinutos", 60L);

        service.enviarVerificacao("maria@exemplo.com", "TOKEN123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void emailTemLinkConfiguravelPrazoEPropositoDaMensagem() {
        SimpleMailMessage m = enviar("https://app.exemplo.com", 24);

        assertEquals("no-reply@exemplo.com", m.getFrom());
        assertEquals("maria@exemplo.com", m.getTo()[0]);
        assertTrue(m.getText().contains("https://app.exemplo.com/verificar-email?token=TOKEN123"));
        assertTrue(m.getText().contains("24 horas"));
        assertTrue(m.getText().contains("confirmar este e-mail"));
    }

    @Test
    void barraFinalNaUrlDoFrontendNaoGeraBarraDupla() {
        SimpleMailMessage m = enviar("http://localhost:3000/", 12);

        assertTrue(m.getText().contains("http://localhost:3000/verificar-email?token=TOKEN123"));
        assertTrue(m.getText().contains("12 horas"));
    }
}
