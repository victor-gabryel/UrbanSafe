package com.urbansafe.autenticacao;

import com.urbansafe.usuario.Usuario;
import com.urbansafe.usuario.UsuarioRepository;
import com.urbansafe.seguranca.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(
                request.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException(
                    "E-mail ou senha inválidos.");
        }

        if (!"ATIVO".equals(usuario.getStatus())) {
            throw new BadCredentialsException(
                    "Não foi possível acessar esta conta.");
        }

        String token = jwtService.gerarToken(usuario);

        return new TokenResponse(
                token,
                "Bearer",
                jwtService.getTempoExpiracaoSegundos(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().getNome());
    }
}