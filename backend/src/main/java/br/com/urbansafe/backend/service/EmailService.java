package br.com.urbansafe.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String remetente;

    @Value("${app.frontend.url}")
    private String urlFrontend;

    @Value("${app.password-reset.expiration-minutes}")
    private long expiracaoRecuperacaoMinutos;

    @Value("${app.email-verification.expiration-hours:24}")
    private long expiracaoVerificacaoHoras;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarVerificacao(String email, String token) {
        String link = montarLink("/verificar-email", token);

        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Verifique seu e-mail - UrbanSafe");

        mensagem.setText(
                "Olá!\n\n"
                + "Obrigado por se cadastrar no UrbanSafe.\n\n"
                + "Para confirmar este e-mail e ativar sua conta, acesse o link abaixo:\n\n"
                + link + "\n\n"
                + "Este link expira em " + expiracaoVerificacaoHoras + " horas e só pode ser usado uma vez.\n\n"
                + "Se você não solicitou este cadastro, ignore esta mensagem."
        );

        mailSender.send(mensagem);
    }

    public void enviarRecuperacaoSenha(String email, String token) {
        String link = montarLink("/redefinir-senha", token);

        SimpleMailMessage mensagem = new SimpleMailMessage();

        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Redefinição de senha - UrbanSafe");

        mensagem.setText(
                "Olá!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta no UrbanSafe.\n\n"
                + "Para escolher uma nova senha, acesse o link abaixo:\n\n"
                + link + "\n\n"
                + "Este link expira em " + expiracaoRecuperacaoMinutos + " minutos e só pode ser usado uma vez.\n\n"
                + "Se você não fez este pedido, ignore esta mensagem: sua senha continua a mesma."
        );

        mailSender.send(mensagem);
    }

    private String montarLink(String caminho, String token) {
        String base = urlFrontend.endsWith("/")
                ? urlFrontend.substring(0, urlFrontend.length() - 1)
                : urlFrontend;

        return base + caminho + "?token=" + token;
    }
}
