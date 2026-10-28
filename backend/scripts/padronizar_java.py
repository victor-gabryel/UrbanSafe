#!/usr/bin/env python3
"""Padroniza formatação e imports dos arquivos Java do backend.

Uso (a partir da raiz do projeto, onde está o pom.xml):

    python scripts/padronizar_java.py            # aplica as correções
    python scripts/padronizar_java.py --check    # só verifica (exit 1 se algo mudaria)

Somente a biblioteca padrão do Python 3.9+ é necessária.

O que é padronizado (mesmas regras do .editorconfig e do checkstyle.xml):

  * UTF-8 sem BOM, fim de linha LF, exatamente um LF no fim do arquivo.
  * Indentação de 4 espaços (tabs viram 4 espaços; arquivos que usavam
    unidade de 8 espaços são reduzidos para 4, preservando a estrutura).
  * Sem espaços no fim das linhas.
  * No máximo uma linha em branco seguida; sem linha em branco logo após '(' ou
    ',', entre anotação e declaração, após '{' de bloco ou antes de '}'.
    Declarações de tipo (class/interface/record/enum) têm uma linha em branco
    após a '{' de abertura, exceto quando o corpo é vazio.
  * Reparo de "espaçamento duplo" (uma linha em branco inserida após cada linha
    de código), detectado apenas em arquivos com sinais inequívocos disso.
  * Imports em quatro blocos separados por uma linha em branco:
        1. import static ...        (todos, em ordem ASCII)
        2. java.* e javax.*
        3. bibliotecas de terceiros (jakarta.*, org.*, io.*, com.*, ...)
        4. br.com.urbansafe.*
    Cada bloco em ordem ASCII; duplicatas e imports sem nenhuma ocorrência do
    nome simples no restante do arquivo (nem em comentários/Javadoc) são
    removidos.

O script nunca altera lógica: só espaço em branco, comentários não são
reescritos e a ordem/remoção de imports segue as regras acima.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SOURCE_DIRS = ("src/main/java", "src/test/java")
INTERNAL_PREFIX = "br.com.urbansafe."
NATIVE_PREFIXES = ("java.", "javax.")

IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.]+(?:\.\*)?)\s*;\s*$")
TYPE_DECL_RE = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*"
    r"(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp)\s+)*"
    r"(?:class|interface|enum|record|@interface)\s+\w+"
)


# --------------------------------------------------------------------------- #
# Etapa 1: texto bruto -> linhas normalizadas
# --------------------------------------------------------------------------- #
def normalizar_texto(texto: str) -> list[str]:
    if texto.startswith("\ufeff"):
        texto = texto[1:]
    texto = texto.replace("\r\r\n", "\n").replace("\r\n", "\n").replace("\r", "\n")
    linhas = []
    for linha in texto.split("\n"):
        linha = linha.rstrip(" \t")
        m = re.match(r"^\t+", linha)
        if m:
            linha = "    " * len(m.group(0)) + linha[m.end() :]
        linhas.append(linha)
    return linhas


# --------------------------------------------------------------------------- #
# Etapa 2: reparo de espaçamento duplo
# --------------------------------------------------------------------------- #
def _dentro_de_comentario_de_bloco(linha: str) -> bool:
    s = linha.strip()
    if s.startswith("*/"):
        return False
    return s.startswith("*") or (s.startswith("/*") and "*/" not in s)


def _linha_com_comentario(linha: str) -> bool:
    """Linha de comentário (ou com comentário no fim): não serve de evidência
    sobre continuação de instrução."""
    s = linha.strip()
    return s.startswith(("//", "*", "/*")) or "//" in s or "*/" in s


def _tem_branco_impossivel(linhas: list[str]) -> bool:
    """Linha em branco onde ninguém escreveria uma: dentro de parênteses/lista
    de argumentos ou entre anotação e declaração."""
    for i in range(1, len(linhas) - 1):
        if linhas[i].strip() == "" and linhas[i - 1].strip() and linhas[i + 1].strip():
            a = linhas[i - 1].rstrip()
            sa = a.lstrip()
            if _linha_com_comentario(a):
                continue
            if a.endswith(("(", ",", "+", "->", ".")):
                return True
            if (
                sa.startswith("@")
                and not a.endswith((";", "{", "}"))
                and a.count("(") == a.count(")")
            ):
                return True
    return False


def reparar_espacamento_duplo(linhas: list[str]) -> list[str]:
    if not _tem_branco_impossivel(linhas):
        return linhas

    # Mantém o cabeçalho (package/imports) intacto; os imports são reconstruídos depois.
    ultimo = -1
    for i, l in enumerate(linhas):
        if l.startswith(("package ", "import ")):
            ultimo = i
    cabecalho, corpo = linhas[: ultimo + 1], linhas[ultimo + 1 :]

    saida: list[str] = []
    i = 0
    while i < len(corpo):
        linha = corpo[i]
        if linha.strip():
            saida.append(linha)
            i += 1
            continue
        j = i
        while j < len(corpo) and not corpo[j].strip():
            j += 1
        k = j - i
        anterior = saida[-1] if saida else ""
        if not anterior.strip() or _dentro_de_comentario_de_bloco(anterior):
            originais = k
        else:
            originais = (k - 1) // 2
        saida.extend([""] * originais)
        i = j
    return cabecalho + saida


# --------------------------------------------------------------------------- #
# Etapa 3: indentação (unidade de 8 -> 4)
# --------------------------------------------------------------------------- #
def _recuo(linha: str) -> int:
    return len(linha) - len(linha.lstrip(" "))


def detectar_unidade(linhas: list[str]) -> int:
    """Retorna 8 se o arquivo usa unidade de 8 espaços, senão 4."""
    codigo = [
        l
        for l in linhas
        if l.strip()
        and not l.lstrip().startswith(("*", "/*", "//", "package ", "import "))
    ]
    recuos = [_recuo(l) for l in codigo if _recuo(l) > 0]
    if not recuos:
        return 4
    if any(r % 8 != 0 for r in recuos):
        return 4

    # Recuo do primeiro membro dentro do corpo do primeiro tipo (se houver).
    estado = "procura"  # procura -> cabecalho -> corpo
    for l in codigo:
        s = l.strip()
        if estado == "procura":
            if TYPE_DECL_RE.match(s):
                estado = "corpo" if s.endswith("{") else "cabecalho"
            continue
        if estado == "cabecalho":
            if s.endswith("{"):
                estado = "corpo"
            continue
        # estado == "corpo": primeira linha de código depois da '{' de abertura
        if _recuo(l) > 0:
            return 8 if _recuo(l) == 8 else 4
        break
    # Sem corpo: record/anotação só com componentes. Componentes em 16 = unidade 8.
    return 8 if min(recuos) >= 16 else 4


def reindentar(linhas: list[str], unidade: int) -> list[str]:
    if unidade == 4:
        return linhas
    saida = []
    for l in linhas:
        if not l.strip():
            saida.append(l)
            continue
        n = _recuo(l)
        if l.lstrip().startswith("*") and n % 8 == 1:  # continuação de Javadoc
            novo = (n - 1) // 2 + 1
        else:
            novo = n // 2
        saida.append(" " * novo + l.lstrip(" "))
    return saida


# --------------------------------------------------------------------------- #
# Etapa 4: linhas em branco
# --------------------------------------------------------------------------- #
def _anotacao_completa(linha: str) -> bool:
    s = linha.strip()
    return (
        s.startswith("@")
        and not s.startswith("@interface")
        and s.count("(") == s.count(")")
        and not s.endswith((",", ";", "{", "}", "+"))
    )


def normalizar_brancos(linhas: list[str]) -> list[str]:
    # Remove brancos nas bordas e colapsa sequências.
    seq: list[str] = []
    for l in linhas:
        if l.strip() == "" and (not seq or seq[-1].strip() == ""):
            continue
        seq.append(l)
    while seq and not seq[-1].strip():
        seq.pop()

    def proxima_nao_vazia(idx: int) -> str:
        j = idx + 1
        while j < len(seq) and not seq[j].strip():
            j += 1
        return seq[j] if j < len(seq) else ""

    saida: list[str] = []
    for idx, l in enumerate(seq):
        if l.strip() == "":
            prev = saida[-1] if saida else ""
            ps, ns = prev.strip(), proxima_nao_vazia(idx).strip()
            comentario = _linha_com_comentario(prev)
            # Dentro de /* ... */ uma linha vazia nunca é legítima (seria " *").
            if _dentro_de_comentario_de_bloco(prev) and ns.startswith("*"):
                continue
            if not comentario and ps.endswith(("(", ",", "+", "->", ".", "=")):
                continue
            if ns.startswith(")"):
                continue
            if not comentario and _anotacao_completa(prev):
                continue
            if not comentario and ps.endswith("{") and not saida_e_tipo(saida):
                continue
            if ns.startswith("}"):
                continue
        saida.append(l)

    # Uma linha em branco após '{' de declaração de tipo (exceto corpo vazio).
    final: list[str] = []
    i = 0
    while i < len(saida):
        l = saida[i]
        final.append(l)
        if l.rstrip().endswith("{") and saida_e_tipo(saida[: i + 1]):
            prox = saida[i + 1] if i + 1 < len(saida) else ""
            if prox.strip() and not prox.strip().startswith("}"):
                final.append("")
        i += 1
    return final


def saida_e_tipo(linhas_ate_aqui: list[str]) -> bool:
    """A última linha termina a '{' de uma declaração de tipo?"""
    if not linhas_ate_aqui or not linhas_ate_aqui[-1].rstrip().endswith("{"):
        return False
    # Sobe até o início da instrução (linha anterior terminada em ';', '{', '}' ou vazia/comentário).
    j = len(linhas_ate_aqui) - 1
    inicio = j
    while j >= 0:
        s = linhas_ate_aqui[j].strip()
        if j != len(linhas_ate_aqui) - 1 and (
            s == "" or s.endswith((";", "{", "}")) or s.startswith(("//", "*", "/*"))
        ):
            break
        inicio = j
        j -= 1
    texto = " ".join(l.strip() for l in linhas_ate_aqui[inicio:])
    # Descarta anotações iniciais e testa o formato de declaração de tipo.
    return bool(TYPE_DECL_RE.match(re.sub(r"^(?:@\w+(?:\([^)]*\))?\s*)+", "", texto)))


def _estado_por_linha(linhas: list[str]) -> list[tuple[bool, bool]]:
    """Para cada linha: (começa no nível de membro de um tipo?, fecha um bloco de membro?).

    Percorre o texto ignorando strings, caracteres e comentários, mantendo uma
    pilha de blocos ('type' para class/interface/enum/record, 'other' nos demais).
    """
    pilha: list[str] = []
    buffer = ""
    em_bloco_comentario = False
    estados: list[tuple[bool, bool]] = []
    for linha in linhas:
        nivel_membro = all(k == "type" for k in pilha)
        i, n = 0, len(linha)
        while i < n:
            if em_bloco_comentario:
                j = linha.find("*/", i)
                if j < 0:
                    i = n
                else:
                    em_bloco_comentario = False
                    i = j + 2
                continue
            c = linha[i]
            if c == '"' or c == "'":
                j = i + 1
                while j < n and linha[j] != c:
                    j += 2 if linha[j] == "\\" else 1
                buffer += '""'
                i = j + 1
            elif linha.startswith("//", i):
                i = n
            elif linha.startswith("/*", i):
                em_bloco_comentario = True
                i += 2
            elif c == "{":
                cabecalho = re.sub(r"^(?:@\w+(?:\([^)]*\))?\s*)+", "", buffer.strip())
                pilha.append("type" if TYPE_DECL_RE.match(cabecalho) else "other")
                buffer = ""
                i += 1
            elif c == "}":
                if pilha:
                    pilha.pop()
                buffer = ""
                i += 1
            elif c == ";":
                buffer = ""
                i += 1
            else:
                buffer += c
                i += 1
        fecha_membro = linha.strip() == "}" and all(k == "type" for k in pilha)
        estados.append((nivel_membro, fecha_membro))
    return estados


def _parece_metodo(linhas: list[str], inicio: int) -> bool:
    """A partir de `inicio`, pula anotações/comentários e diz se o próximo
    membro é método, construtor, inicializador ou tipo (e não um campo)."""
    j = inicio
    while j < len(linhas):
        s = linhas[j].strip()
        if s == "" or s.startswith(("@", "//", "*", "/*")):
            j += 1
            continue
        if s.endswith("{"):
            return True
        antes_do_parentese = s.split("(")[0]
        return "(" in s and "=" not in antes_do_parentese and not s.endswith(";")
    return False


def separar_membros(linhas: list[str]) -> list[str]:
    estados = _estado_por_linha(linhas)
    saida: list[str] = []
    for i, linha in enumerate(linhas):
        saida.append(linha)
        if i + 1 >= len(linhas) or not linha.strip() or not linhas[i + 1].strip():
            continue
        prox = linhas[i + 1].strip()
        nivel_membro, fecha_membro = estados[i]
        if prox.startswith(("}", ")")):
            continue
        if fecha_membro:
            saida.append("")
        elif (
            nivel_membro
            and estados[i + 1][0]
            and linha.rstrip().endswith(";")
            and not linha.strip().startswith(("import ", "package "))
            and _parece_metodo(linhas, i + 1)
        ):
            saida.append("")
    return saida


# --------------------------------------------------------------------------- #
# Etapa 5: imports
# --------------------------------------------------------------------------- #
def _grupo(nome: str, estatico: bool) -> int:
    if estatico:
        return 0
    if nome.startswith(NATIVE_PREFIXES):
        return 1
    if nome.startswith(INTERNAL_PREFIX):
        return 3
    return 2


def organizar_imports(linhas: list[str]) -> list[str]:
    idx_imports = [i for i, l in enumerate(linhas) if IMPORT_RE.match(l.strip())]
    if not idx_imports:
        return linhas

    # package + (eventuais comentários antes dos imports) permanecem; imports saem do lugar.
    primeiro, ultimo = idx_imports[0], idx_imports[-1]
    intervalo = linhas[primeiro : ultimo + 1]
    nao_import = [l for l in intervalo if l.strip() and not IMPORT_RE.match(l.strip())]
    if nao_import:
        # Comentário ou código misturado no meio dos imports: não arrisca.
        return linhas

    antes, depois = linhas[:primeiro], linhas[ultimo + 1 :]
    corpo = "\n".join(antes + depois)

    vistos: dict[tuple[bool, str], None] = {}
    for i in idx_imports:
        m = IMPORT_RE.match(linhas[i].strip())
        vistos[(bool(m.group(1)), m.group(2))] = None

    mantidos: list[tuple[bool, str]] = []
    for estatico, nome in vistos:
        simples = nome.rsplit(".", 1)[-1]
        if simples == "*" or re.search(rf"\b{re.escape(simples)}\b", corpo):
            mantidos.append((estatico, nome))

    blocos: dict[int, list[str]] = {0: [], 1: [], 2: [], 3: []}
    for estatico, nome in mantidos:
        blocos[_grupo(nome, estatico)].append(
            ("import static " if estatico else "import ") + nome + ";"
        )
    # ordem ASCII pelo nome qualificado (sem a palavra 'static')
    for g in blocos:
        blocos[g].sort(
            key=lambda l: l.replace("import static ", "")
            .replace("import ", "")
            .rstrip(";")
        )

    novo_cabecalho: list[str] = []
    for g in (0, 1, 2, 3):
        if blocos[g]:
            if novo_cabecalho:
                novo_cabecalho.append("")
            novo_cabecalho.extend(blocos[g])

    # Reconstrói: [antes (package)] + '' + imports + '' + [depois]
    antes = [l for l in antes]
    while antes and not antes[-1].strip():
        antes.pop()
    depois = list(depois)
    while depois and not depois[0].strip():
        depois.pop(0)
    partes = antes
    if novo_cabecalho:
        partes = partes + [""] + novo_cabecalho
    if depois:
        partes = partes + [""] + depois
    return partes


# --------------------------------------------------------------------------- #
# Orquestração
# --------------------------------------------------------------------------- #
def padronizar(texto: str) -> str:
    linhas = normalizar_texto(texto)
    linhas = reparar_espacamento_duplo(linhas)
    linhas = reindentar(linhas, detectar_unidade(linhas))
    linhas = organizar_imports(linhas)
    linhas = normalizar_brancos(linhas)
    linhas = normalizar_brancos(separar_membros(linhas))
    return "\n".join(linhas) + "\n"


def arquivos_java(raiz: Path):
    for d in SOURCE_DIRS:
        base = raiz / d
        if base.is_dir():
            yield from sorted(base.rglob("*.java"))


def main() -> int:
    ap = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    ap.add_argument(
        "--check",
        action="store_true",
        help="não grava; sai com 1 se algum arquivo mudaria",
    )
    ap.add_argument(
        "--raiz", default=".", help="raiz do projeto (padrão: diretório atual)"
    )
    args = ap.parse_args()

    raiz = Path(args.raiz)
    alterados = []
    total = 0
    for arq in arquivos_java(raiz):
        total += 1
        bruto = arq.read_bytes()
        original = bruto.decode("utf-8")
        novo = padronizar(original)
        if novo.encode("utf-8") != bruto:
            alterados.append(arq)
            if not args.check:
                arq.write_bytes(novo.encode("utf-8"))

    rotulo = "precisam de padronização" if args.check else "padronizados"
    print(f"{len(alterados)} de {total} arquivos {rotulo}.")
    for a in alterados:
        print("  ", a.as_posix())
    return 1 if (args.check and alterados) else 0


if __name__ == "__main__":
    sys.exit(main())