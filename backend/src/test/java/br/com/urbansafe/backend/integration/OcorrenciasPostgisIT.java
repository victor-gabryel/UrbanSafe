package br.com.urbansafe.backend.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.com.urbansafe.backend.entity.Usuario;
import br.com.urbansafe.backend.repository.PerfilRepository;
import br.com.urbansafe.backend.repository.UsuarioRepository;
import br.com.urbansafe.backend.security.JwtService;

/**
 * Teste de integração contra um PostgreSQL/PostGIS REAL (as migrations V1–V6 são aplicadas pelo Flyway).
 * Só roda quando URBANSAFE_IT_DB_URL está definida; use um banco DESCARTÁVEL, pois o teste insere dados e
 * os registros de auditoria são imutáveis (não há limpeza). Exemplo:
 * <pre>
 * URBANSAFE_IT_DB_URL=jdbc:postgresql://localhost:5432/urbansafe_it URBANSAFE_IT_DB_USER=urbansafe \
 * URBANSAFE_IT_DB_PASSWORD=... ./mvnw test -Dtest=OcorrenciasPostgisIT
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "URBANSAFE_IT_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${URBANSAFE_IT_DB_URL}",
        "spring.datasource.username=${URBANSAFE_IT_DB_USER:urbansafe}",
        "spring.datasource.password=${URBANSAFE_IT_DB_PASSWORD:}",
        "app.jwt.secret=segredo-de-teste-com-mais-de-32-bytes-0123456789",
        "spring.mail.host=localhost", "spring.mail.username=x", "spring.mail.password=x",
        "app.mail.from=teste@urbansafe.local",
        "app.storage.local.path=${java.io.tmpdir}/urbansafe-it-storage"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OcorrenciasPostgisIT {
    // Região isolada (longitude 100.1, latitude 50.1) para não colidir com dados existentes.
    private static final String BBOX = "100.0,50.0,100.2,50.2";
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PerfilRepository perfilRepository;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbc;

    MockMvc mvc;
    Usuario autor, outro, gestor, admin;
    String tAutor, tOutro, tGestor, tAdmin;
    int tipoId;

    @BeforeAll
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();

        autor = criarUsuario("USUARIO");
        outro = criarUsuario("USUARIO");
        gestor = criarUsuario("GESTOR");
        admin = criarUsuario("ADMINISTRADOR");
        tAutor = "Bearer " + jwtService.gerarAccessToken(autor);
        tOutro = "Bearer " + jwtService.gerarAccessToken(outro);
        tGestor = "Bearer " + jwtService.gerarAccessToken(gestor);
        tAdmin = "Bearer " + jwtService.gerarAccessToken(admin);

        String corpo = mvc.perform(post("/api/tipos-ocorrencia").header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Tipo IT " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        tipoId = JsonPath.read(corpo, "$.id");
    }

    private Usuario criarUsuario(String perfil) {
        Usuario u = new Usuario();
        u.setNome("IT " + perfil);
        u.setEmail("it-" + UUID.randomUUID() + "@urbansafe.local");
        u.setSenhaHash("$2a$10$" + "x".repeat(53));
        u.setPerfil(perfilRepository.findByNome(perfil).orElseThrow());
        u.setEmailVerificado(true);
        return usuarioRepository.save(u);
    }

    private int novoTipo() throws Exception {
        String corpo = mvc.perform(post("/api/tipos-ocorrencia").header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Tipo IT " + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    private int criarOcorrencia(String token, double lat, double lon) throws Exception {
        return criarOcorrencia(token, lat, lon, tipoId);
    }

    private int criarOcorrencia(String token, double lat, double lon, int tipo) throws Exception {
        String corpo = mvc.perform(post("/api/ocorrencias").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipoOcorrenciaId\":" + tipo + ",\"titulo\":\"t\",\"descricao\":\"d\","
                        + "\"latitude\":" + lat + ",\"longitude\":" + lon + ",\"usuarioId\":999,\"status\":\"PUBLICADA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE")) // status do corpo é ignorado
                .andExpect(jsonPath("$.autorId").value(autor.getId())) // usuarioId do corpo é ignorado
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    private void aprovar(int id) throws Exception {
        mvc.perform(post("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tGestor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"APROVAR\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void semTokenRetorna401EComTokenSemPermissaoRetorna403() throws Exception {
        mvc.perform(get("/api/ocorrencias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/mapa/calor?bbox=" + BBOX)).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/tipos-ocorrencia").header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tipos-ocorrencia").header("Authorization", tGestor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/auditoria").header("Authorization", tGestor)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/auditoria").header("Authorization", tAdmin)).andExpect(status().isOk());
    }

    @Test
    void validaCoordenadasEPaginacao() throws Exception {
        for (String coords : new String[] { "\"latitude\":91,\"longitude\":0", "\"latitude\":0,\"longitude\":-181" }) {
            mvc.perform(post("/api/ocorrencias").header("Authorization", tAutor)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tipoOcorrenciaId\":" + tipoId + ",\"titulo\":\"t\",\"descricao\":\"d\"," + coords + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/ocorrencias?limit=101").header("Authorization", tAutor)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ocorrencias?page=0").header("Authorization", tAutor)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ocorrencias?bbox=1,2,3").header("Authorization", tAutor)).andExpect(status().isBadRequest());
    }

    @Test
    void bboxUsaPostgisEFiltraPorStatusETipo() throws Exception {
        int tipo = novoTipo();
        int dentro = criarOcorrencia(tAutor, 50.1, 100.1, tipo);
        int fora = criarOcorrencia(tAutor, -23.5, -46.6, tipo);

        // PENDENTE: o mapa público não mostra
        mvc.perform(get("/api/mapa/ocorrencias?bbox=" + BBOX + "&tipo=" + tipo).header("Authorization", tOutro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(0));

        aprovar(dentro);
        aprovar(fora);

        mvc.perform(get("/api/mapa/ocorrencias?bbox=" + BBOX + "&tipo=" + tipo).header("Authorization", tOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(dentro))
                .andExpect(jsonPath("$.itens[0].autorId").doesNotExist())
                .andExpect(jsonPath("$.itens[0].descricao").doesNotExist());

        mvc.perform(get("/api/ocorrencias?bbox=" + BBOX + "&tipo=" + tipo + "&page=1&limit=10")
                .header("Authorization", tOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].autorId").doesNotExist()); // anonimização para terceiros

        mvc.perform(get("/api/mapa/calor?bbox=" + BBOX + "&celula=0.01&tipo=" + tipo).header("Authorization", tOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos.length()").value(1))
                .andExpect(jsonPath("$.pontos[0].quantidade").value(1))
                .andExpect(jsonPath("$.aviso").exists());

        // período que não contém o registro
        mvc.perform(get("/api/ocorrencias?bbox=" + BBOX + "&tipo=" + tipo + "&dataFim=2000-01-01T00:00:00Z").header("Authorization", tOutro))
                .andExpect(jsonPath("$.totalItens").value(0));

        // usuário comum não pede status não público; gestor pode
        mvc.perform(get("/api/mapa/ocorrencias?bbox=" + BBOX + "&status=PENDENTE").header("Authorization", tOutro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/mapa/ocorrencias?bbox=" + BBOX + "&status=PENDENTE").header("Authorization", tGestor))
                .andExpect(status().isOk());

        // índice espacial existe e é GiST
        Integer gist = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_ocorrencias_localizacao_gist' "
                        + "AND indexdef ILIKE '%USING gist%'", Integer.class);
        assertTrue(gist != null && gist == 1);
    }

    @Test
    void propriedadeVisibilidadeModeracaoEHistorico() throws Exception {
        int id = criarOcorrencia(tAutor, 50.15, 100.15);

        // PENDENTE de outra pessoa é invisível (404); o autor e o gestor veem
        mvc.perform(get("/api/ocorrencias/{id}", id).header("Authorization", tOutro)).andExpect(status().isNotFound());
        mvc.perform(get("/api/ocorrencias/{id}", id).header("Authorization", tAutor)).andExpect(status().isOk());
        mvc.perform(get("/api/ocorrencias/{id}", id).header("Authorization", tGestor)).andExpect(status().isOk());

        // usuário comum não modera, nem consulta moderação
        mvc.perform(post("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"APROVAR\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tAutor)).andExpect(status().isForbidden());

        // PATCH parcial do autor não apaga campos não enviados; status no corpo é ignorado
        mvc.perform(patch("/api/ocorrencias/{id}", id).header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"novo\",\"status\":\"PUBLICADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("novo"))
                .andExpect(jsonPath("$.descricao").value("d"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        // justificativa obrigatória e transição inválida
        mvc.perform(post("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tGestor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"REJEITAR\"}")).andExpect(status().isBadRequest());
        aprovar(id);
        mvc.perform(post("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tGestor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"APROVAR\"}")).andExpect(status().isConflict());

        // publicada: outro usuário vê, mas não edita nem exclui; o autor não edita mais
        mvc.perform(get("/api/ocorrencias/{id}", id).header("Authorization", tOutro)).andExpect(status().isOk());
        mvc.perform(patch("/api/ocorrencias/{id}", id).header("Authorization", tOutro)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"hack\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/ocorrencias/{id}", id).header("Authorization", tOutro)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/ocorrencias/{id}", id).header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"x\"}")).andExpect(status().isConflict());

        // histórico: autor e gestor consultam; outro usuário comum não
        mvc.perform(get("/api/ocorrencias/{id}/historico", id).header("Authorization", tAutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].acao").value("CRIACAO"));
        mvc.perform(get("/api/ocorrencias/{id}/historico", id).header("Authorization", tOutro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ocorrencias/{id}/moderacao", id).header("Authorization", tGestor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // auditoria imutável no banco
        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE historico_ocorrencia SET descricao = 'x' WHERE ocorrencia_id = ?", id));
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM moderacoes WHERE ocorrencia_id = ?", id));
    }

    @Test
    void midiasValidamConteudoEPermissoes() throws Exception {
        int id = criarOcorrencia(tAutor, 50.12, 100.12);

        String corpo = mvc.perform(multipart("/api/ocorrencias/{id}/midias", id)
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG)).header("Authorization", tAutor))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("IMAGEM"))
                .andReturn().getResponse().getContentAsString();
        int midiaId = JsonPath.read(corpo, "$.id");

        mvc.perform(multipart("/api/ocorrencias/{id}/midias", id)
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", "<?php ?>".getBytes()))
                .header("Authorization", tAutor)).andExpect(status().isUnsupportedMediaType());
        mvc.perform(multipart("/api/ocorrencias/{id}/midias", id)
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG)).header("Authorization", tOutro))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/ocorrencias/{id}/midias/{m}/arquivo", id, midiaId).header("Authorization", tAutor))
                .andExpect(status().isOk());
        mvc.perform(get("/api/ocorrencias/{id}/midias/{m}/arquivo", id, midiaId).header("Authorization", tOutro))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/ocorrencias/{id}/midias/{m}", id, midiaId).header("Authorization", tAutor))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ocorrencias/{id}/midias", id).header("Authorization", tAutor))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void escalonamentoDePrivilegiosEBloqueado() throws Exception {
        String alvo = "{\"perfil\":\"GESTOR\"}";

        mvc.perform(patch("/api/admin/usuarios/{id}/perfil", outro.getId()).header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content(alvo)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/usuarios/{id}/perfil", autor.getId()).header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/usuarios/{id}/perfil", outro.getId()).header("Authorization", tGestor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/usuarios/{id}/perfil", admin.getId()).header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"USUARIO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/usuarios/{id}/perfil", outro.getId()).header("Authorization", tAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"SUPER\"}"))
                .andExpect(status().isBadRequest());
        // o corpo do cadastro/atualização de perfil próprio não aceita "perfil"
        mvc.perform(patch("/api/usuarios/me").header("Authorization", tAutor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Novo\",\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("USUARIO"));
    }
}
