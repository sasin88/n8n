#!/usr/bin/env python3
"""
Genera el catálogo nutricional de NutriAI a partir de USDA FoodData Central (SR Legacy).

- Solo copia valores publicados por USDA (dominio público). Si un alimento no se encuentra,
  se omite y se informa: nunca se inventan valores.
- Las recetas se calculan sumando sus componentes; su composición es aproximada y queda marcada.

Uso: python3 build_catalog.py <salida.json> [ruta_zip_sr_legacy]
"""
import csv
import io
import json
import re
import sys
import urllib.request
import zipfile

ZIP_URLS = [
    "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip",
    "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_%202018-04.zip",
    "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2019-04-02.zip",
]

NUTRIENTS = {
    "kcal": ("Energy", "KCAL"),
    "protein": ("Protein", "G"),
    "fat": ("Total lipid (fat)", "G"),
    "carbs": ("Carbohydrate, by difference", "G"),
    "fiber": ("Fiber, total dietary", "G"),
}

ML_PER = {"CUP": 236.5882, "TABLESPOON": 14.7868, "TEASPOON": 4.9289}


def download():
    for url in ZIP_URLS:
        try:
            print(f"Descargando {url}")
            with urllib.request.urlopen(url, timeout=120) as r:
                return r.read()
        except Exception as e:  # noqa: BLE001
            print(f"  falló: {e}")
    sys.exit("No se pudo descargar SR Legacy desde ninguna URL conocida")


def read_csv(z, name):
    path = next((n for n in z.namelist() if n.endswith("/" + name) or n == name), None)
    if path is None:
        sys.exit(f"{name} no está en el zip")
    with z.open(path) as f:
        return list(csv.DictReader(io.TextIOWrapper(f, encoding="utf-8")))


def portion_unit(text):
    t = text.lower()
    if re.search(r"\bfl oz\b", t):
        return "MILLILITER"
    if re.search(r"\bcups?\b", t):
        return "CUP"
    if re.search(r"\btbsp\b|tablespoon", t):
        return "TABLESPOON"
    if re.search(r"\btsp\b|teaspoon", t):
        return "TEASPOON"
    if re.search(r"\bslices?\b", t):
        return "SLICE"
    if re.search(r"\bpieces?\b", t):
        return "PIECE"
    if re.search(r"\b(medium|large|small|each|whole|fruit|breast|thigh|egg|tortilla|patty|chop|fillet|can|bottle|container|bar)\b", t):
        return "UNIT"
    return None


def main():
    out_path = sys.argv[1]
    data = open(sys.argv[2], "rb").read() if len(sys.argv) > 2 else download()
    z = zipfile.ZipFile(io.BytesIO(data))

    foods = read_csv(z, "food.csv")
    nutrients = read_csv(z, "nutrient.csv")
    food_nutrients = read_csv(z, "food_nutrient.csv")
    portions = read_csv(z, "food_portion.csv")

    nutrient_ids = {}
    for key, (name, unit) in NUTRIENTS.items():
        match = [n["id"] for n in nutrients if n["name"] == name and n["unit_name"].upper() == unit]
        if not match:
            sys.exit(f"Nutriente {name} no encontrado")
        nutrient_ids[match[0]] = key

    values = {}
    for fn in food_nutrients:
        key = nutrient_ids.get(fn["nutrient_id"])
        if key:
            values.setdefault(fn["fdc_id"], {})[key] = float(fn["amount"])

    portions_by_food = {}
    for p in portions:
        portions_by_food.setdefault(p["fdc_id"], []).append(p)

    by_desc = {f["description"]: f for f in foods}
    spec = json.load(open(sys.argv[0].rsplit("/", 1)[0] + "/foods_spec.json", encoding="utf-8"))

    def find(item):
        for d in item.get("desc", []):
            if d in by_desc:
                return by_desc[d]
        need = [w.lower() for w in item.get("all", [])]
        avoid = [w.lower() for w in item.get("none", [])]
        if not need:
            return None
        cands = [
            f for f in foods
            if all(w in f["description"].lower() for w in need)
            and not any(w in f["description"].lower() for w in avoid)
        ]
        return min(cands, key=lambda f: len(f["description"])) if cands else None

    out_foods, missing = [], []
    by_id = {}
    for item in spec["foods"]:
        f = find(item)
        v = values.get(f["fdc_id"], {}) if f else {}
        if not f or any(k not in v for k in ("kcal", "protein", "fat", "carbs")):
            missing.append(item["id"])
            continue
        eq = []
        seen = set()
        for p in portions_by_food.get(f["fdc_id"], []):
            text = " ".join(x for x in (p.get("modifier", ""), p.get("portion_description", "")) if x).strip()
            unit = portion_unit(text)
            try:
                amount = float(p.get("amount") or 1) or 1
                grams = float(p["gram_weight"]) / amount
            except (ValueError, ZeroDivisionError):
                continue
            if unit == "MILLILITER":
                grams = grams / 29.5735  # 1 fl oz US = 29.5735 ml
            label = f"USDA: 1 {text}"
            if unit and unit not in seen and grams > 0:
                seen.add(unit)
                eq.append({"unit": unit, "grams": round(grams, 2), "label": label})
        entry = {
            "id": item["id"],
            "name": item["es"],
            "category": item["cat"],
            "aliases": item.get("aliases", []),
            "source": "USDA_FDC",
            "sourceReference": f'FDC {f["fdc_id"]} · {f["description"]}',
            "per100g": {k: round(v[k], 2) for k in ("kcal", "protein", "carbs", "fat")} | {"fiber": round(v["fiber"], 2) if "fiber" in v else None},
            "portions": eq,
        }
        out_foods.append(entry)
        by_id[item["id"]] = entry
        print(f'OK  {item["id"]:<26} <- FDC {f["fdc_id"]} {f["description"]}')

    out_recipes = []
    for r in spec["recipes"]:
        comps = r["components"]
        absent = [c for c, _ in comps if c not in by_id]
        if absent:
            missing.append(f'{r["id"]} (faltan {", ".join(absent)})')
            continue
        out_recipes.append({
            "id": r["id"],
            "name": r["es"],
            "countryCode": r.get("country"),
            "aliases": r.get("aliases", []),
            "components": [{"foodId": c, "grams": g} for c, g in comps],
        })

    json.dump(
        {
            "version": 1,
            "source": "USDA FoodData Central, SR Legacy (dominio público). Recetas: composición aproximada calculada por NutriAI.",
            "foods": out_foods,
            "recipes": out_recipes,
        },
        open(out_path, "w", encoding="utf-8"),
        ensure_ascii=False,
        indent=1,
    )
    print(f"\n{len(out_foods)} alimentos, {len(out_recipes)} recetas")
    if missing:
        print("NO ENCONTRADOS (omitidos):", ", ".join(missing))


if __name__ == "__main__":
    main()
