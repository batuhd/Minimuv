#!/usr/bin/env python3
"""Minimuv audit workflow çıktısını journal'dan toplar ve rapor için normalize eder.

Yalnızca okuma yapar; çıktıyı audit-reports/ altına yazar.
Workflow script'indeki dedup/verdict mantığını birebir taklit eder.
"""
import json
import os
import re
import sys

WF_DIR = "/home/batu/.claude/projects/-home-batu-StudioProjects-Minimuv/2ab717a7-0712-4b8d-ab3a-b6b7faf25a7d/subagents/workflows/wf_dc42d1d2-1ab"
OUT = "/home/batu/StudioProjects/Minimuv/audit-reports/_tools"

ORDER = [
    "core-bildirim", "core-arama-ag", "data-katmani", "uygulama-tema",
    "bilesenler", "kutuphane-liste", "detay-ekran", "detay-kartlar",
    "ekle-kisi-studyo", "cark-rozet", "profil-ayarlar", "istatistik",
    "altyapi-yapilandirma", "supabase", "dead-code", "duplicate",
    "guvenlik", "performans", "kalite-test-a11y", "bagimlilik-tedarik",
]


def norm(s):
    return re.sub(r"[^a-z0-9çğıöşü]+", " ", str(s or "").lower()).strip()


def sev_rank(s):
    return {"Kritik": 5, "Yüksek": 4, "Orta": 3, "Düşük": 2, "Bilgi": 1}.get(s, 0)


def load_meta():
    labels = {}
    for f in os.listdir(WF_DIR):
        if f.endswith(".meta.json"):
            path = os.path.join(WF_DIR, f)
            with open(path, encoding="utf-8") as fh:
                d = json.load(fh)
            aid = f[len("agent-"):-len(".meta.json")]
            labels[aid] = d.get("description", "")
    return labels


def main():
    labels = load_meta()
    results = {}          # label -> result object
    verdicts = {}         # ref -> verdict object
    extra_verdicts = {}
    critic = None
    failures = []
    started = []
    with open(os.path.join(WF_DIR, "journal.jsonl"), encoding="utf-8") as fh:
        for line in fh:
            try:
                r = json.loads(line)
            except Exception:
                continue
            t = r.get("type")
            aid = r.get("agentId", "")
            label = labels.get(aid, "")
            if t == "started":
                started.append(label)
            elif t == "failed":
                failures.append(label)
            elif t == "result":
                res = r.get("result") or {}
                if label.startswith("verify-extra:"):
                    for v in res.get("verdicts", []):
                        extra_verdicts[v.get("ref")] = v
                elif label.startswith("verify:"):
                    for v in res.get("verdicts", []):
                        verdicts[v.get("ref")] = v
                elif label == "completeness-critic":
                    critic = res
                elif label:
                    # Ajan etiketleri "audit:" / "cross:" önekli; ORDER öneksiz.
                    key = label.split(":", 1)[1] if label.startswith(("audit:", "cross:")) else label
                    results[key] = res

    all_findings = []
    per_label = {}
    for label in ORDER:
        res = results.get(label)
        if not res:
            continue
        fs = res.get("findings") or []
        per_label[label] = len(fs)
        all_findings.extend(fs)

    deduped = []
    for f in all_findings:
        nt = norm(f.get("title"))
        dup = None
        for o in deduped:
            if o.get("category") != f.get("category") or o.get("file") != f.get("file"):
                continue
            if norm(o.get("title")) == nt or (
                abs((o.get("startLine") or 0) - (f.get("startLine") or 0)) <= 4
                and nt[:28] == norm(o.get("title"))[:28]
            ):
                dup = o
                break
        if dup:
            dup.setdefault("relatedLocations", []).extend(f.get("relatedLocations") or [])
            if sev_rank(f.get("severity")) > sev_rank(dup.get("severity")):
                dup["severity"] = f.get("severity")
            if len(str(f.get("description", ""))) + len(str(f.get("evidence", ""))) > \
               len(str(dup.get("description", ""))) + len(str(dup.get("evidence", ""))):
                for k in ("description", "impact", "evidence", "suggestion",
                          "startLine", "endLine", "confidence", "status"):
                    dup[k] = f.get(k)
        else:
            g = dict(f)
            g["relatedLocations"] = list(f.get("relatedLocations") or [])
            deduped.append(g)

    for i, f in enumerate(deduped, 1):
        f["_ref"] = "F%d" % i

    verified, dropped = [], []
    for f in deduped:
        v = verdicts.get(f["_ref"])
        if not v or v.get("verdict") in ("Yanlış", "Doğrulanamadı"):
            dropped.append({"ref": f["_ref"], "title": f.get("title"), "file": f.get("file"),
                            "verdict": v.get("verdict") if v else "DoğrulamaYok",
                            "reason": v.get("reason") if v else "verify sonucu yok"})
            continue
        c = dict(f)
        if (v.get("correctedStartLine") or 0) > 0:
            c["startLine"] = v["correctedStartLine"]
        if (v.get("correctedEndLine") or 0) > 0:
            c["endLine"] = v["correctedEndLine"]
        if str(v.get("correctedEvidence") or "").strip():
            c["evidence"] = v["correctedEvidence"]
        if v.get("correctedSeverity"):
            c["severity"] = v["correctedSeverity"]
        c["verification"] = v.get("verdict")
        c["verificationReason"] = v.get("reason")
        verified.append(c)

    # Bütünlük ajanı ek bulguları
    extras = []
    extra_raw = (critic or {}).get("findings") or []
    if extra_raw:
        by_file = {}
        for f in extra_raw:
            by_file.setdefault(f.get("file"), []).append(f)
        for file, fs in by_file.items():
            for n, f in enumerate(fs, 1):
                ref = "X%d_%s" % (n, re.sub(r"[^a-zA-Z0-9]", "_", str(file))[-20:])
                v = extra_verdicts.get(ref)
                if not v or v.get("verdict") in ("Yanlış", "Doğrulanamadı"):
                    continue
                c = dict(f)
                if (v.get("correctedStartLine") or 0) > 0:
                    c["startLine"] = v["correctedStartLine"]
                if (v.get("correctedEndLine") or 0) > 0:
                    c["endLine"] = v["correctedEndLine"]
                c["verification"] = v.get("verdict")
                c["verificationReason"] = v.get("reason")
                extras.append(c)

    final = verified + extras
    for f in final:
        f.pop("_ref", None)

    counts = {}
    for f in final:
        c = counts.setdefault(f.get("category", "?"), {"total": 0})
        c["total"] += 1
        c[f.get("severity", "?")] = c.get(f.get("severity", "?"), 0) + 1

    out = {
        "stats": {
            "raw": len(all_findings), "deduped": len(deduped), "verified": len(verified),
            "extras": len(extras), "dropped": len(dropped),
            "per_label": per_label, "categories": counts,
            "started": started, "failures": failures,
        },
        "dropped": dropped,
        "findings": final,
        "critic_notes": (critic or {}).get("notes", ""),
    }
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "findings.json")
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)

    print(json.dumps(out["stats"], ensure_ascii=False, indent=2))
    print("yazıldı:", path)


if __name__ == "__main__":
    main()
