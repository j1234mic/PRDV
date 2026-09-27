#!/usr/bin/env python3
"""Controle de coherence croisee du module 2 (sans JVM disponible).

Verifications :
  1. package declare == repertoire du fichier
  2. tout `import com.prdv.rdv.*` resout (fichier ou type imbrique)
  3. toute interface metier est integralement implementee (nom + arite)
  4. tout `new X(...)` sur un record du depot a la bonne arite
  5. constantes d'enum `Enum.CONST` existantes
  6. requetes derivees Spring Data (`findByPropAndProp`) sur des proprietes existantes
  7. `@Convert(converter = ...)` pointe sur un convertisseur existant
"""
from __future__ import annotations

import pathlib
import re
import sys
from collections import defaultdict

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "src/main/java"
TESTS = ROOT / "src/test/java"

TYPE_DECL = re.compile(
    r"\b(class|interface|enum|record|@interface)\s+([A-Z]\w*)")
ENUM_CONST = re.compile(r"^\s*([A-Z][A-Z0-9_]*)\s*(?:\(|,|;|$)")
METHOD_DECL = re.compile(
    r"(?:^|\n)[ \t]*(?:(?:public|protected|private|static|final|abstract|default|"
    r"synchronized|native)\s+)*"
    r"(?:<[^>]+>\s+)?"
    r"([A-Za-z_][\w\<\>\[\],\.\?\s]*?)\s+"
    r"([a-z]\w*)\s*\(([^;{)]*(?:\([^)]*\)[^;{)]*)*)\)\s*"
    r"(?:throws [\w\.,\s]+)?[{;]")
RECORD_HEADER = re.compile(r"\brecord\s+([A-Z]\w*)\s*\(")
IMPLEMENTS = re.compile(r"\b(?:implements|extends)\s+([^{]+)\{")
FIELD_DECL = re.compile(
    r"(?:^|\n)[ \t]*(?:private|protected|public)\s+(?:static\s+|final\s+)*"
    r"([\w\<\>\[\],\.\?\s]+?)\s+(\w+)\s*(?:=[^;]+)?;")


def strip_noise(source: str) -> str:
    """Retire commentaires et litteraux en un seul passage gauche-droite.

    Un retrait par expressions regulieres separees casse des qu'une chaine
    contient {@code //} ou {@code /*} (ex. {@code "otpauth://totp/%s"}) : tout
    le reste du fichier disparait et les methodes suivantes ne sont plus vues.
    """
    out: list[str] = []
    index, size = 0, len(source)
    while index < size:
        char = source[index]
        following = source[index + 1] if index + 1 < size else ""
        if char == "/" and following == "/":
            while index < size and source[index] != "\n":
                index += 1
        elif char == "/" and following == "*":
            index += 2
            while index + 1 < size and not (source[index] == "*" and source[index + 1] == "/"):
                index += 1
            index += 2
            out.append(" ")
        elif char == '"':
            if source.startswith('"""', index):
                end = source.find('"""', index + 3)
                index = end + 3 if end != -1 else size
            else:
                index += 1
                while index < size and source[index] != '"':
                    if source[index] == "\\":
                        index += 1
                    index += 1
                index += 1
            out.append('""')
        elif char == "'":
            index += 1
            while index < size and source[index] != "'":
                if source[index] == "\\":
                    index += 1
                index += 1
            index += 1
            out.append("' '")
        else:
            out.append(char)
            index += 1
    return "".join(out)


def top_level_split(text: str, sep: str = ",") -> list[str]:
    """Decoupe au separateur de niveau 0.

    La fleche de lambda est neutralisee avant comptage (sinon le '>' de '->'
    ferait passer les virgules d'une lambda au niveau 0) ; la profondeur ne
    descend jamais sous zero pour rester robuste aux '>' de comparaison.
    """
    text = text.replace("->", "  ")
    parts, depth, current = [], 0, []
    for char in text:
        if char in "([{<":
            depth += 1
        elif char in ")]}>":
            depth = max(0, depth - 1)
        if char == sep and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(char)
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return [p.strip() for p in parts if p.strip()]


def balanced(text: str, start: int) -> tuple[str, int]:
    """Contenu entre parentheses a partir de l'index de '(' ; retourne (contenu, index_apres)."""
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:index], index + 1
    return text[start + 1:], len(text)


class Type:
    def __init__(self, name: str, kind: str, fqn: str, file: pathlib.Path):
        self.name = name
        self.kind = kind
        self.fqn = fqn
        self.file = file
        self.methods: dict[str, set[int]] = defaultdict(set)
        self.supers: list[str] = []
        self.components: list[str] | None = None
        self.enum_constants: set[str] = set()
        self.fields: set[str] = set()
        self.nested: list[str] = []


def index_sources() -> tuple[dict[str, Type], dict[pathlib.Path, str]]:
    types: dict[str, Type] = {}
    raw: dict[pathlib.Path, str] = {}
    for file in sorted(list(SRC.rglob("*.java")) + list(TESTS.rglob("*.java"))):
        text = file.read_text(encoding="utf-8")
        raw[file] = text
        package = re.search(r"^package\s+([\w\.]+);", text, flags=re.M)
        package = package.group(1) if package else ""
        clean = strip_noise(text)

        declared = list(TYPE_DECL.finditer(clean))
        if not declared:
            continue
        for position, match in enumerate(declared):
            kind, name = match.group(1), match.group(2)
            end = declared[position + 1].start() if position + 1 < len(declared) else len(clean)
            body = clean[match.end():end]
            outer = declared[position - 1].group(2) if position > 0 else None
            qualifier = f"{package}.{outer}" if (package and outer) else (package or "")
            fqn = f"{qualifier}.{name}" if qualifier else name
            entry = types.setdefault(fqn, Type(name, kind, fqn, file))
            if outer:
                outer_fqn = f"{package}.{outer}" if package else outer
                types.setdefault(
                    outer_fqn,
                    Type(outer, declared[position - 1].group(1), outer_fqn, file)).nested.append(name)

            for method in METHOD_DECL.finditer(body):
                params = method.group(3).strip()
                arity = 0 if not params else len(top_level_split(params))
                entry.methods[method.group(2)].add(arity)

            supers = IMPLEMENTS.search(clean[match.start():end])
            if supers:
                entry.supers.extend(s.strip() for s in top_level_split(supers.group(1)))

            for field in FIELD_DECL.finditer(body):
                entry.fields.add(field.group(2))

            if kind == "record":
                paren = clean.find("(", match.end())
                if paren != -1:
                    params, _ = balanced(clean, paren)
                    entry.components = [part.strip().split()[-1]
                                        for part in top_level_split(params) if part.strip()]

            if kind == "enum":
                brace = body.find("{")
                if brace >= 0:
                    entry.enum_constants.update(enum_constants(body[brace + 1:]))
    return types, raw


def enum_constants(body: str) -> set[str]:
    """Constantes d'une enum : tout ce qui precede le premier ';' hors parentheses."""
    prefix, depth = [], 0
    for char in body:
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        elif char == ";" and depth == 0:
            break
        prefix.append(char)
    text = "".join(prefix)
    text = re.sub(r"\([^)]*\)", " ", text)          # arguments de constructeur
    text = re.sub(r"\{[^}]*\}", " ", text)          # corps de constante anonymous
    return {part.strip().split()[0] for part in text.split(",")
            if part.strip() and re.match(r"^[A-Z][A-Z0-9_]*", part.strip())}


def main() -> int:
    types, raw = index_sources()
    by_simple: dict[str, list[str]] = defaultdict(list)
    for fqn in types:
        by_simple[fqn.rsplit(".", 1)[-1]].append(fqn)
    problems: list[str] = []

    # ---------------------------------------------------------------- 1. package
    for file, text in raw.items():
        declared = re.search(r"^package\s+([\w\.]+);", text, flags=re.M)
        if not declared:
            problems.append(f"{file}: declaration de package absente")
            continue
        base = SRC if str(file).startswith(str(SRC)) else TESTS
        expected = str(file.parent.relative_to(base)).replace("/", ".")
        if declared.group(1) != expected:
            problems.append(f"{file}: package {declared.group(1)} != repertoire {expected}")

    # ---------------------------------------------------------------- 2. imports
    for file, text in raw.items():
        for import_line in re.findall(r"^import\s+(static\s+)?(com\.prdv\.rdv\.[\w\.]+);",
                                      text, flags=re.M):
            target = import_line[1]
            parts = target.split(".")
            resolved = False
            for cut in range(len(parts), 1, -1):
                candidate = next((root.joinpath(*parts[:cut]).with_suffix(".java")
                                  for root in (SRC, TESTS)
                                  if root.joinpath(*parts[:cut]).with_suffix(".java").exists()), None)
                if candidate is not None:
                    rest = parts[cut:]
                    if not rest:
                        resolved = True
                    else:
                        body = strip_noise(raw[candidate])
                        resolved = all(
                            re.search(rf"\b(?:class|interface|enum|record)\s+{seg}\b", body)
                            for seg in rest)
                    break
            if not resolved:
                problems.append(f"{file.name}: import non resolu {target}")

    # ---------------------------------------------------------------- 3. interfaces
    for fqn, entry in types.items():
        if entry.kind != "class":
            continue
        available: dict[str, set[int]] = defaultdict(set)
        for name, arities in entry.methods.items():
            available[name].update(arities)
        # Seules les super-CLASSES apportent des implementations ; ajouter les
        # methodes des interfaces rendrait la verification triviale.
        for parent in entry.supers:
            simple = parent.split("<")[0].strip().rsplit(".", 1)[-1]
            for candidate in by_simple.get(simple, []):
                target = types[candidate]
                if target.kind == "class":
                    for name, arities in target.methods.items():
                        available[name].update(arities)
        for interface in entry.supers:
            simple = interface.split("<")[0].strip().rsplit(".", 1)[-1]
            for candidate in by_simple.get(simple, []):
                target = types[candidate]
                if target.kind != "interface":
                    continue
                for name, arities in target.methods.items():
                    for arity in arities:
                        if arity not in available.get(name, set()):
                            problems.append(
                                f"{entry.name} n'implemente pas {simple}.{name}/{arity}")

    # ---------------------------------------------------------------- 4. records
    # Les records imbriques portent souvent le meme nom d'un holder a l'autre
    # (UpdateDmpSharing existe dans PatientIdentityCommands ET PrivacyCommands) :
    # on resout donc le nom qualifie, et on s'abstient en cas d'ambiguite.
    records_by_name: dict[str, list[str]] = defaultdict(list)
    for fqn, entry in types.items():
        if entry.kind == "record":
            records_by_name[fqn.rsplit(".", 1)[-1]].append(fqn)

    def resolve_record(reference: str) -> str | None:
        parts = reference.split(".")
        candidates = records_by_name.get(parts[-1], [])
        if len(parts) > 1:
            qualified = [fqn for fqn in candidates
                         if fqn.split(".")[-2:] == parts[-2:]]
            return qualified[0] if len(qualified) == 1 else None
        return candidates[0] if len(candidates) == 1 else None

    for file, text in raw.items():
        if "com/prdv/rdv/profile" not in str(file).replace("\\", "/"):
            continue
        clean = strip_noise(text)
        for match in re.finditer(r"new\s+([A-Z][\w\.]*)\s*\(", clean):
            fqn = resolve_record(match.group(1))
            if not fqn:
                continue
            params, _ = balanced(clean, match.end() - 1)
            arity = 0 if not params.strip() else len(top_level_split(params))
            expected = types[fqn].components
            if expected is not None and arity != len(expected):
                problems.append(
                    f"{file.name}: new {match.group(1)}(.../{arity}) alors que le record attend "
                    f"{len(expected)} composants")

    # ---------------------------------------------------------------- 5. enums
    enum_names = {fqn.rsplit(".", 1)[-1]: types[fqn]
                  for fqn, entry in types.items() if entry.kind == "enum"}
    for file, text in raw.items():
        if "com/prdv/rdv/profile" not in str(file).replace("\\", "/"):
            continue
        clean = strip_noise(text)
        for match in re.finditer(r"\b([A-Z][A-Za-z0-9]*)\.([A-Z][A-Z0-9_]*)\b", clean):
            owner, constant = match.group(1), match.group(2)
            entry = enum_names.get(owner)
            if entry and entry.enum_constants and constant not in entry.enum_constants:
                problems.append(f"{file.name}: {owner}.{constant} inexistant "
                                f"(disponibles : {sorted(entry.enum_constants)[:8]}...)")

    # ---------------------------------------------------------------- 6. requetes derivees
    entity_files = {}
    for file in raw:
        entity_files[file.stem] = file

    def inherited_fields(entity_name: str) -> set[str]:
        fields: set[str] = set()
        seen: set[str] = set()
        queue = [entity_name]
        while queue:
            current = queue.pop()
            if current in seen:
                continue
            seen.add(current)
            file = entity_files.get(current)
            if file is None:
                continue
            clean = strip_noise(raw[file])
            fields.update(field.group(2) for field in FIELD_DECL.finditer(clean))
            parent = re.search(r"\bclass\s+" + current + r"[^{]*\bextends\s+(\w+)", clean)
            if parent:
                queue.append(parent.group(1))
        return fields
    for file, text in raw.items():
        if "Repository" not in file.stem or "persistence/repository" not in str(file):
            continue
        generic = re.search(r"JpaRepository<\s*(\w+)\s*,", text)
        if not generic:
            continue
        entity_name = generic.group(1)
        entity_file = entity_files.get(entity_name)
        if entity_file is None:
            problems.append(f"{file.name}: entite introuvable {entity_name}")
            continue
        properties = inherited_fields(entity_name)
        for match in re.finditer(
                r"\b(?:find|delete|count|exists)(?:All)?(?:First\d+|Top\d+)?By([A-Z]\w*)\s*\(", text):
            method = match.group(1)
            # Une methode portee par @Query n'obeit pas a la convention de nommage
            if "@Query" in text[max(0, match.start() - 300):match.start()]:
                continue
            properties_part = method.split("OrderBy")[0]
            for property_part in re.split(r"(?<=\w)(?:And|Or)(?=[A-Z])", properties_part):
                property_part = re.sub(
                    r"(IgnoreCase|IsNotNull|IsNull|NotNull|Null|Before|After|Between|LessThan"
                    r"(Equal)?|GreaterThan(Equal)?|NotIn|In|NotLike|Like|Containing|StartingWith"
                    r"|EndingWith|True|False|Equals|Not|Exists)$", "", property_part)
                if not property_part:
                    continue
                property_name = property_part[0].lower() + property_part[1:]
                if property_name not in properties:
                    problems.append(f"{file.name}: propriete inconnue '{property_name}' "
                                    f"(entite {entity_name})")

    # ---------------------------------------------------------------- 7. convertisseurs
    converters = {t.name for t in types.values() if t.kind == "class"}
    for file, text in raw.items():
        for used in re.findall(r"@Convert\(converter\s*=\s*(?:JsonConverters\.)?(\w+)\.class", text):
            if used not in converters:
                problems.append(f"{file.name}: convertisseur introuvable {used}")

    # ------------------------------------------------- 8. getters/setters d'entites
    # Les mappers JPA manipulent des entites Lombok : un getter/setteur fantome ne
    # se revele qu'a la compilation. On verifie chaque acces contre les champs.
    entity_properties: dict[str, set[str]] = {}
    for file in raw:
        if "persistence/entity" not in str(file).replace("\\", "/"):
            continue
        clean = strip_noise(raw[file])
        props = {field.group(2) for field in FIELD_DECL.finditer(clean)}
        for parent in re.findall(r"\bclass\s+\w+[^{]*\bextends\s+(\w+)", clean):
            parent_file = entity_files.get(parent)
            if parent_file is not None:
                props.update(field.group(2) for field
                             in FIELD_DECL.finditer(strip_noise(raw[parent_file])))
        entity_properties[file.stem] = props

    for file in raw:
        if "persistence/mapper" not in str(file).replace("\\", "/"):
            continue
        clean = strip_noise(raw[file])
        # le type manipule est celui du parametre/retour des methodes toEntity/toDomain
        present = [name for name in entity_properties if name in clean]
        if not present:
            continue
        known = set().union(*(entity_properties[name] for name in present))
        for accessor, name in re.findall(r"\b(?:entity|saved|existing)\.(get|set)([A-Z]\w*)\s*\(", clean):
            prop = name[0].lower() + name[1:]
            if prop not in known:
                problems.append(f"{file.name}: acces {accessor}{name}() sans champ '{prop}' "
                                f"sur {present}")

    print(f"types indexes : {len(types)} | fichiers : {len(raw)}")
    for problem in problems:
        print("  -", problem)
    print(f"problemes : {len(problems)}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
