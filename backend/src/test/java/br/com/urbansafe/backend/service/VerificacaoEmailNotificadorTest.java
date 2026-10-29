package br.com.urbansafe.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class VerificacaoEmailNotificadorTest {

    private EmailService emailService;
    private VerificacaoEmailNotificador notificador;

    @BeforeEach
    void preparar() {
        emailService = mock(EmailService.class);
        notificador = new VerificacaoEmailNotificador(emailService);
    }

    @AfterEach
    void limpar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void foraDeTransacaoEnviaNaHora() {
        notificador.enviarAposCommit(1, "a@b.com", "tok");

        verify(emailService).enviarVerificacao("a@b.com", "tok");
    }

    @Test
    void dentroDeTransacaoSoEnviaDepoisDoCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notificador.enviarAposCommit(1, "a@b.com", "tok");

        verify(emailService, never()).enviarVerificacao("a@b.com", "tok");

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(emailService).enviarVerificacao("a@b.com", "tok");
    }

    @Test
    void transacaoDesfeitaNaoEnviaEmail() {
        TransactionSynchronizationManager.initSynchronization();

        notificador.enviarAposCommit(1, "a@b.com", "tok");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(emailService, never()).enviarVerificacao("a@b.com", "tok");
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void falhaDeSmtpNaoPropagaExcecao() {
        doThrow(new MailSendException("smtp fora do ar")).when(emailService).enviarVerificacao("a@b.com", "tok");

        assertDoesNotThrow(() -> notificador.enviarAposCommit(1, "a@b.com", "tok"));
    }
}