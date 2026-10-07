#!/usr/bin/env python3
"""Check shipping language resources, fallbacks and reviewed player-text sinks."""
import collections
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'common/src/main/resources'
JAVA = ROOT / 'common/src/main/java/dev/thaumcraft'
TEXT_OWNERS = {
    'item/ArcanaItem.java', 'item/EqualTrade.java', 'item/RelicActions.java',
    'entity/TravelingTrunk.java', 'machine/MachineBlock.java', 'world/EldritchBlock.java',
    'client/ResearchScreen.java', 'client/MachineScreen.java', 'client/TrunkScreen.java',
    'client/VoidScreen.java', 'client/ArcaneHud.java',
    'jei/ResearchCategory.java', 'jei/InfusionCategory.java', 'jei/JeiIngredients.java',
    'command/ThaumcraftCommand.java',
}
# Full key coverage is required for Ukrainian, every official EU language and the
# EEA additions, in the core mod and both Alt addon language folders. Other
# language files may stay partial and fall back to English.
COMPLETE_LANGUAGES = {
    'uk_ua',
    'bg_bg', 'hr_hr', 'cs_cz', 'da_dk', 'nl_nl', 'et_ee', 'fi_fi', 'fr_fr', 'de_de', 'el_gr',
    'hu_hu', 'ga_ie', 'it_it', 'lv_lv', 'lt_lt', 'mt_mt', 'pl_pl', 'pt_pt', 'ro_ro', 'sk_sk',
    'sl_si', 'es_es', 'sv_se',  # EU official languages (English is the source)
    'is_is', 'no_no', 'nn_no',  # EEA: Iceland and Norway; Liechtenstein uses German
}


def check_languages(directory, label):
    """Validate every language file in one folder against its en_us.json."""
    english = read(directory / 'en_us.json')
    for code in sorted(COMPLETE_LANGUAGES):
        assert (directory / f'{code}.json').is_file(), f'Missing {label} language file: {code}'
    for path in sorted(directory.glob('*.json')):
        values = read(path)
        assert not values.keys() - english.keys(), f'Obsolete keys in {path}: {values.keys() - english.keys()}'
        if path.stem in COMPLETE_LANGUAGES:
            assert values.keys() == english.keys(), f'Missing {path.stem} keys in {label}: {english.keys() - values.keys()}'
        for key, value in values.items():
            assert isinstance(value, str), f'Non-string translation: {path}:{key}'
            assert value.strip() or not english[key].strip(), f'Empty translation: {path}:{key}'
            assert placeholders(value) == placeholders(english[key]), f'Changed placeholders: {path}:{key}'
    return english


def unique(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, f'Duplicate JSON key: {key}'
        result[key] = value
    return result


def read(path):
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)


def placeholders(value):
    # Minecraft supports %s, positional %N$s and %% (not printf numeric formats).
    slots, next_slot = [], 1
    for match in re.finditer(r'%(?:(\d+)\$)?([s%])|(%.)|(%$)', value):
        assert match.group(3) is None and match.group(4) is None, f'Invalid placeholder: {value}'
        if match.group(2) == '%':
            assert match.group(1) is None, value
            continue
        slot = int(match.group(1)) if match.group(1) else next_slot
        assert slot >= 1, f'Invalid argument index: {value}'
        slots.append(slot)
        if match.group(1) is None:
            next_slot += 1
    return collections.Counter(slots)


def verify():
    en = check_languages(RES / 'assets/thaumcraft2tp/lang', 'core')

    for project in read(RES / 'thaumcraft2tp/gameplay.json')['research']:
        for field in ('name', 'text'):
            key = project[field + '_key']
            assert en[key] == project[field].replace('\\n', '\n'), f'Changed research fallback: {key}'

    addon = ROOT / 'examples/alts-tc2tp-patches'
    gating = addon
    check_languages(addon / 'common/src/main/resources/assets/alts_tc2tp_patches/lang', 'addon')
    check_languages(gating / 'assets/research_gating/lang', 'research gating')

    research_en = read(gating / 'assets/research_gating/lang/en_us.json')
    required_keys = set()
    for path in sorted((gating / 'data/research_gating/thaumcraft2tp/research').glob('*.json')):
        project = read(path)
        for field in ('name', 'text'):
            key = project[field + '_key']
            required_keys.add(key)
            assert key == f'research.research_gating.{path.stem}.{field}', f'Changed research key: {path}:{key}'
            assert research_en[key] == project[field].replace('\\n', '\n'), f'Changed research fallback: {key}'
    assert research_en.keys() == required_keys, 'Missing or obsolete research-gating translations'
    core_projects = {p['id'].split(':', 1)[1]: p for p in read(RES / 'thaumcraft2tp/gameplay.json')['research']}
    for path in sorted((gating / 'data/thaumcraft2tp/thaumcraft2tp/research').glob('*.json')):
        project, original = read(path), core_projects[path.stem]
        for field in ('name', 'text'):
            assert project[field + '_key'] == original[field + '_key'], f'Lost core translation key: {path}:{field}'
            assert project[field] == original[field], f'Changed core research fallback: {path}:{field}'
    for path in (addon / 'common/src/main/java').rglob('*.java'):
        for key in re.findall(r'"((?:message|tooltip)\.alts_tc2tp_patches\.[a-z0-9_/.]+)"', path.read_text()):
            assert key in read(addon / 'common/src/main/resources/assets/alts_tc2tp_patches/lang/en_us.json'), f'Missing addon key: {path}:{key}'
    for suffix, fallback in [('lost','Lost'),('forbidden','Forbidden'),('tainted','Tainted'),('eldritch','Eldritch')]:
        assert en['category.thaumcraft2tp.' + suffix] == fallback

    descriptions = read(RES / 'thaumcraft2tp/seals.json')
    seal_source = (JAVA / 'machine/SealLogic.java').read_text()
    descriptions.update(dict(re.findall(r'case "([0-5,-]+)" -> "([^"]*)";', seal_source)))
    for combination, value in descriptions.items():
        if value.strip():
            key = 'seal.thaumcraft2tp.combination.' + combination.replace('-1','none').replace(',','_')
            assert en[key] == 'This seal ' + value, f'Changed effective seal fallback: {key}'
    for i in range(6):
        assert f'tooltip.thaumcraft2tp.difficulty.{i}' in en
    for i in range(7):
        assert f'tooltip.thaumcraft2tp.network.{i}' in en
    for category in ('lost','forbidden','tainted','eldritch'):
        for rarity in ('common','uncommon','rare','exceptional'):
            assert f'tooltip.thaumcraft2tp.artifact.{category}.{rarity}' in en

    quoted = r'"(?:[^"\\]|\\.)*"'
    for path in JAVA.rglob('*.java'):
        source = path.read_text()
        for match in re.finditer(r'Component\.translatableWithFallback\(('+quoted+r'),\s*('+quoted+r')', source):
            key, fallback = map(json.loads, match.groups())
            if key.startswith(('message.','tooltip.','gui.','jei.','commands.','item.')):
                assert en.get(key) == fallback, f'Missing key or fallback mismatch: {path}:{key}'
        for key in re.findall(r'"((?:message|tooltip|gui|jei|commands|item|category|seal)\.thaumcraft2tp\.[a-z0-9_/.]+)"', source):
            assert key in en or key.endswith('.') and any(k.startswith(key) for k in en), f'Missing key: {path}:{key}'
        # Only the reviewed text owners reject new literal labels. Logs and the
        # hidden written-book pages are not player-text sinks.
        if path.relative_to(JAVA).as_posix() in TEXT_OWNERS:
            assert not re.search(r'Component\.literal\("[A-Za-z]', source), f'Visible English literal: {path}'
            assert not re.search(r'(?:g|graphics)\.text(?:WithWordWrap)?\([^,]+,\s*"[A-Za-z]', source), f'Raw screen label: {path}'
    print(f'Localization verified: {len(en)} core keys in {len(COMPLETE_LANGUAGES) + 1} complete languages (EU, EEA, Ukrainian and English), {len(required_keys) // 2} addon research definitions, 25 core overrides, addon text, effective seal descriptions, placeholders and text sinks.')


if __name__ == '__main__':
    verify()
