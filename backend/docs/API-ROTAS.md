# API de rotas e exposição

Documento de referência: seções 3.7, 15.2.7 e 16.1 do "Escopo UrbanSafe".

## POST /api/rotas/calcular (público)

Calcula um percurso **a pé** ou **de bicicleta** e, se pedido, o indicador informativo de exposição.
Sem JWT nada é gravado; com JWT a consulta vai para o histórico (tabela `rotas`). Limite por IP (padrão 30 consultas / 10 min → `429`).

```json
{
  "origem":  { "latitude": -8.0476, "longitude": -34.8770, "rotulo": "Marco Zero" },
  "destino": { "latitude": -8.0578, "longitude": -34.8829 },
  "perfil": "A_PE",
  "preferencias": { "considerarOcorrencias": true, "considerarDistanciaDasOcorrencias": true }
}
```

Resposta `200` (resumida): `perfil`, `origem`, `destino`, `distanciaMetros`, `duracaoSegundos`,
`geometria` (`LineString`, coordenadas `[longitude, latitude]`), `exposicao`, `calculadaEm`, `id` (nulo se anônima).

`exposicao`: `indice` (0–100 ou `null`), `ocorrenciasProximas`, `distanciaBuscaMetros`, `periodoDias`,
`dadosAtualizadosEm`, `observacao`, `aviso`, `parametros`.

Erros: `400` (coordenadas inválidas, pontos iguais, distância acima de `ROTAS_DISTANCIA_MAXIMA_KM`), `422` (sem caminho), `429`, `503` (roteamento indisponível).

## Fórmula do indicador (explicável)

Só entram ocorrências **PUBLICADAS** e não excluídas, no PostGIS (`ST_DWithin` sobre `geography`).

```
contribuicao = pesoRelevancia x decaimentoTemporal x fatorProximidade
pesoRelevancia     BAIXA=1  MEDIA=2  ALTA=3
decaimentoTemporal 0,5 ^ (idadeEmDias / meiaVidaDias)            (padrão: meia-vida 60 dias)
fatorProximidade   1 - distancia / raio                          (raio padrão 150 m; 1 se desligado)
soma   = Σ contribuicao                                          (concentração: mais ocorrências, soma maior)
indice = 100 x (1 - e^(-soma / escala))                          (escala padrão 6; satura em 100)
```

Ocorrências mais antigas que `periodo-dias` (180) são ignoradas. Se não há nenhuma ocorrência publicada
em até 2 km do percurso, `indice = null` com `observacao` ("sem dados suficientes"), pois um 0 enganaria.

**Os pesos e parâmetros são provisórios** (seção 10.2 do documento): avalie-os antes de apresentar o resultado como
classificação. O indicador depende dos registros disponíveis e **não** é probabilidade de crime nem garantia de segurança.

## Outros endpoints

| Método | Endpoint | Acesso | Finalidade |
|---|---|---|---|
| GET | /api/rotas | autenticado | histórico próprio (`page`, `limit`) |
| GET | /api/rotas/{id} | autenticado | consulta própria (404 se de outra pessoa) |
| DELETE | /api/rotas/{id} | autenticado | apaga consulta própria (dado de localização) |
| GET | /api/usuarios/me/rotas | autenticado | mesmo que GET /api/rotas |

## Serviço de roteamento (configuração e limites)

Usa a API HTTP do OSRM. O servidor demo do OSRM só roteia **carros**; por isso os padrões apontam para o serviço público
FOSSGIS (`routed-foot` / `routed-bike`). Esse serviço tem política de uso e limites: para uso além de testes,
hospede um OSRM próprio e troque `ROTAS_OSRM_URL_A_PE` e `ROTAS_OSRM_URL_BICICLETA` (a URL termina antes das coordenadas,
ex.: `http://meu-osrm:5000/route/v1/foot`). Não foi possível validar a chamada real ao serviço no ambiente de desenvolvimento
desta entrega (sem acesso à internet): teste `POST /api/rotas/calcular` no Swagger após subir o backend.
