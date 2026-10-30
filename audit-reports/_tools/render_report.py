#!/usr/bin/env python3
"""Workflow + manuel bulguları birleştirir, numaralandırır ve kategori raporlarını üretir.

Yalnızca audit-reports/ altına yazar. Kaynak dosyalara dokunmaz.
"""
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
REPORTS = os.path.dirname(BASE)
FINDINGS = os.path.join(BASE, "findings.json")
MANUAL = os.path.join(BASE, "manual_findings.json")
MERGED = os.path.join(BASE, "findings_merged.json")
STATS = os.path.join(BASE, "stats.json")

CAT_ORDER = ["BUG", "SEC", "DEAD", "DUP", "PERF", "QUAL"]
CAT_NAME = {
    "BUG": "Buglar ve Mantık Hataları",
    "SEC": "Güvenlik",
    "DEAD": "Ölü Kod",
    "DUP": "Tekrarlanan Kod ve Gereksiz Karmaşıklık",
    "PERF": "Performans",
    "QUAL": "Kod Kalitesi, Bakım ve Erişilebilirlik",
}
CAT_FILE = {
    "BUG": "01-buglar.md",
    "SEC": "02-guvenlik.md",
    "DEAD": "03-dead-code.md",
    "DUP": "04-duplicate.md",
    "PERF": "05-performans.md",
    "QUAL": "06-kalite.md",
}
SEV_ORDER = ["Kritik", "Yüksek", "Orta", "Düşük", "Bilgi"]
SEV_RANK = {s: i for i, s in enumerate(SEV_ORDER)}


def norm(s):
    return re.sub(r"[^a-z0-9çğıöşü]+", " ", str(s or "").lower()).strip()


def key(f):
    return (f.get("category"), f.get("file"))


def dedupe(findings):
    out = []
    for f in findings:
        nt = norm(f.get("title"))
        dup = None
        for o in out:
            if o.get("category") != f.get("category") or o.get("file") != f.get("file"):
                continue
            if norm(o.get("title")) == nt:
                dup = o
                break
            if abs((o.get("startLine") or 0) - (f.get("startLine") or 0)) <= 4 and \
               nt[:30] == norm(o.get("title"))[:30]:
                dup = o
                break
        if dup is None:
            g = dict(f)
            g["relatedLocations"] = list(f.get("relatedLocations") or [])
            out.append(g)
            continue
        # Aynı bulgu: konumları birleştir, daha yüksek önem ve daha uzun metni koru
        known = {(r.get("file"), r.get("startLine")) for r in dup.get("relatedLocations", [])}
        for r in f.get("relatedLocations") or []:
            if (r.get("file"), r.get("startLine")) not in known:
                dup.setdefault("relatedLocations", []).append(r)
        if SEV_RANK.get(f.get("severity"), 9) < SEV_RANK.get(dup.get("severity"), 9):
            dup["severity"] = f.get("severity")
        if len(str(f.get("description", "")) + str(f.get("evidence", ""))) > \
           len(str(dup.get("description", "")) + str(dup.get("evidence", ""))):
            for k in ("description", "impact", "evidence", "suggestion",
                      "startLine", "endLine", "confidence", "status", "source"):
                if f.get(k) is not None:
                    dup[k] = f.get(k)
        if f.get("verification"):
            dup["verification"] = f["verification"]
            dup["verificationReason"] = f.get("verificationReason")
    return out


def load():
    with open(FINDINGS, encoding="utf-8") as fh:
        wf = json.load(fh)
    wf_findings = wf.get("findings") or []
    for f in wf_findings:
        f["source"] = "workflow"
    manual = []
    if os.path.exists(MANUAL):
        with open(MANUAL, encoding="utf-8") as fh:
            manual = json.load(fh)
    return wf, dedupe(wf_findings + manual)


def round_sev(f):
    return SEV_RANK.get(f.get("severity"), 9)


def loc(f):
    s = f.get("startLine") or 0
    e = f.get("endLine") or s
    path = f.get("file") or "?"
    if not s:
        return f"`{path}`"
    if e and e != s:
        return f"`{path}:{s}-{e}`"
    return f"`{path}:{s}`"


def related_lines(f):
    rows = []
    for r in f.get("relatedLocations") or []:
        p = r.get("file") or "?"
        s = r.get("startLine") or 0
        e = r.get("endLine") or s
        note = (r.get("note") or "").strip()
        if s and e and e != s:
            text = f"`{p}:{s}-{e}`"
        elif s:
            text = f"`{p}:{s}`"
        else:
            text = f"`{p}`"
        rows.append(f"  - {text}" + (f" — {note}" if note else ""))
    return rows


def esc(s):
    return str(s or "").strip()


def render_finding(f, fid):
    lines = []
    lines.append(f"### {fid} — {esc(f.get('title'))}")
    lines.append("")
    lines.append(f"- **Önem Derecesi:** {esc(f.get('severity'))}")
    lines.append(f"- **Güven Seviyesi:** {esc(f.get('confidence'))}")
    lines.append(f"- **Konum:** {loc(f)}")
    rel = related_lines(f)
    if rel:
        lines.append("- **İlgili Konumlar:**")
        lines.extend(rel)
    lines.append("")
    lines.append(f"**Sorunun Açıklaması:** {esc(f.get('description'))}")
    lines.append("")
    lines.append(f"**Etki:** {esc(f.get('impact'))}")
    lines.append("")
    lines.append(f"**Kanıt:** {esc(f.get('evidence'))}")
    lines.append("")
    lines.append(f"**Önerilen Çözüm:** {esc(f.get('suggestion'))}")
    lines.append("")
    lines.append(f"- **Düzeltme Zorluğu:** {esc(f.get('fixDifficulty'))} / {esc(f.get('effort'))}")
    lines.append(f"- **Bağımlılıklar:** {esc(f.get('dependencies'))}")
    if f.get("verification"):
        lines.append(f"- **Adversarial Doğrulama:** {esc(f.get('verification'))}"
                     + (f" — {esc(f.get('verificationReason'))}" if f.get("verificationReason") else ""))
    lines.append("")
    return "\n".join(lines)


def main():
    wf, merged = load()
    # suppress.json: başlık eşleşmesiyle bilinçli olarak gizlenen (üst bulguya katılan) kayıtlar
    suppress_path = os.path.join(BASE, "suppress.json")
    suppressed = []
    if os.path.exists(suppress_path):
        with open(suppress_path, encoding="utf-8") as fh:
            suppressed = json.load(fh)
    if suppressed:
        def hit(f):
            nt = norm(f.get("title"))
            for s in suppressed:
                if norm(s) and norm(s) in nt:
                    return True
            return False
        before = len(merged)
        merged = [f for f in merged if not hit(f)]
        print(f"suppress: {before - len(merged)} kayıt gizlendi")

    merged.sort(key=lambda f: (
        CAT_ORDER.index(f.get("category")) if f.get("category") in CAT_ORDER else 99,
        round_sev(f),
        f.get("file") or "",
        f.get("startLine") or 0,
    ))

    counters = {c: 0 for c in CAT_ORDER}
    for f in merged:
        c = f.get("category")
        if c in counters:
            counters[c] += 1
            f["id"] = f"{c}-{counters[c]:03d}"
        else:
            f["id"] = "OTH-000"

    with open(MERGED, "w", encoding="utf-8") as fh:
        json.dump({"findings": merged}, fh, ensure_ascii=False, indent=1)

    stats = {"total": len(merged), "categories": {}, "workflow_raw": len(wf.get("findings") or []),
             "workflow_stats": wf.get("stats")}
    for c in CAT_ORDER:
        sub = [f for f in merged if f.get("category") == c]
        counts = {s: sum(1 for f in sub if f.get("severity") == s) for s in SEV_ORDER}
        stats["categories"][c] = {"total": len(sub), **counts}
    with open(STATS, "w", encoding="utf-8") as fh:
        json.dump(stats, fh, ensure_ascii=False, indent=1)

    for c in CAT_ORDER:
        sub = [f for f in merged if f.get("category") == c]
        out = [f"# {CAT_NAME[c]}", "",
               f"Toplam **{len(sub)}** bulgu. Aşağıdaki liste önem derecesine göre sıralanmıştır.", ""]
        for f in sub:
            out.append(render_finding(f, f["id"]))
        with open(os.path.join(REPORTS, CAT_FILE[c]), "w", encoding="utf-8") as fh:
            fh.write("\n".join(out).rstrip() + "\n")
        print(f"{CAT_FILE[c]}: {len(sub)}")

    print(json.dumps(stats["categories"], ensure_ascii=False, indent=1))
    print("toplam:", len(merged))


if __name__ == "__main__":
    main()
