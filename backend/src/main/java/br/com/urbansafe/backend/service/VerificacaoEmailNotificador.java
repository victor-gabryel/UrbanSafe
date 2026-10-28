package br.com.urbansafe.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Envia o e-mail de verificação somente depois do commit da transação que gravou o token.
 * Assim nunca sai e-mail para uma conta/token que acabou sendo desfeito, e uma falha de SMTP
 * (que não participa da transação do banco) não derruba o cadastro: o usuário usa o reenvio.
 * Fora de transação, envia imediatamente. O token nunca é registrado em log.
 */
@Component
public class VerificacaoEmailNotificador {

    private static final Logger log = LoggerFactory.getLogger(VerificacaoEmailNotificador.class);

    private final EmailService emailService;

    public VerificacaoEmailNotificador(EmailService emailService) {
        this.emailService = emailService;
    }

    public void enviarAposCommit(Integer usuarioId, String email, String token) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(usuarioId, email, token);
                }
            });
        } else {
            enviar(usuarioId, email, token);
        }
    }

    private void enviar(Integer usuarioId, String email, String token) {
        try {
            emailService.enviarVerificacao(email, token);
        } catch (RuntimeException e) {
            // Só id e tipo da exceção: a mensagem do provedor pode conter o endereço do destinatário.
            log.error("Falha ao enviar e-mail de verificação (usuário id={}): {}",
                    usuarioId, e.getClass().getSimpleName());
        }
    }
}
