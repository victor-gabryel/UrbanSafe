package com.urbansafe.verificacao;

import com.urbansafe.usuario.Usuario;
import com.urbansafe.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class VerificacaoEmailService {

    private final CodigoVerificacaoEmailRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String remetente;

    private final SecureRandom random = new SecureRandom();

    public VerificacaoEmailService(
            CodigoVerificacaoEmailRepository codigoRepository,
            UsuarioRepository usuarioRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            @Value("${MAIL_FROM:${spring.mail.username}}")
            String remetente
    ) {
        this.codigoRepository = codigoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.remetente = remetente;
    }

    @Transactional
    public void enviarCodigo(Usuario usuario) {
        String codigo = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );

        OffsetDateTime agora = OffsetDateTime.now();

        codigoRepository
                .findByUsuarioIdAndUsadoFalse(usuario.getId())
                .forEach(anterior -> {
                    anterior.setUsado(true);
                    codigoRepository.save(anterior);
                });

        CodigoVerificacaoEmail registro =
                new CodigoVerificacaoEmail();

        registro.setId(UUID.randomUUID());
        registro.setUsuarioId(usuario.getId());
        registro.setCodigoHash(passwordEncoder.encode(codigo));
        registro.setCriadoEm(agora);
        registro.setExpiraEm(agora.plusMinutes(10));
        registro.setUsado(false);

        codigoRepository.save(registro);

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject("Código de verificação - UrbanSafe");
        mensagem.setText(
                "Olá, " + usuario.getNome() + "!\n\n"
                + "Seu código de verificação do UrbanSafe é: "
                + codigo + "\n\n"
                + "O código expira em 10 minutos. "
                + "Se você não solicitou este cadastro, "
                + "ignore este e-mail."
        );

        mailSender.send(mensagem);
    }

    @Transactional
    public void confirmarCodigo(String email, String codigo) {
        if (email == null || email.isBlank()
                || codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                    "Informe um e-mail e um código válido de 6 dígitos."
            );
        }

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário não encontrado."
                ));

        if ("ATIVO".equalsIgnoreCase(usuario.getStatus())) {
            throw new IllegalArgumentException(
                    "Esta conta já está ativada."
            );
        }

        OffsetDateTime agora = OffsetDateTime.now();

        CodigoVerificacaoEmail registro = codigoRepository
                .findByUsuarioIdAndUsadoFalse(usuario.getId())
                .stream()
                .filter(item ->
                        item.getExpiraEm().isAfter(agora)
                        && passwordEncoder.matches(
                                codigo, item.getCodigoHash()
                        )
                )
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Código inválido ou expirado."
                ));

        registro.setUsado(true);
        codigoRepository.save(registro);

        usuario.setStatus("ATIVO");
        usuarioRepository.save(usuario);
    }
}