import base64
import json
import sys
from pathlib import Path

COVER = {"capa_frontal", "estrela_base", "estrela_topo", "gema_topo"}
STRAP = {"tira_topo", "tira_vertical", "lacre", "gema_lacre"}
GLOW = {"estrela_base", "estrela_topo", "gema_topo", "gema_lacre"}
FACES = ("north", "south", "east", "west", "up", "down")


def group_of(name):
    if name in COVER or name.startswith("canto_sup"):
        return "cover"
    if name in STRAP:
        return "strap"
    return "body"


def main(source, out_dir):
    data = json.loads(Path(source).read_text(encoding="utf-8"))
    out = Path(out_dir)
    (out / "textures" / "gui").mkdir(parents=True, exist_ok=True)
    (out / "models").mkdir(parents=True, exist_ok=True)

    names = ["grimorio.png", "grimorio_aura.png"]
    for texture, name in zip(data["textures"], names):
        raw = base64.b64decode(texture["source"].split(",", 1)[1])
        (out / "textures" / "gui" / name).write_bytes(raw)

    cubes = []
    for element in data["elements"]:
        faces = {}
        for face in FACES:
            entry = element["faces"].get(face)
            if entry and entry.get("texture") is not None:
                faces[face] = entry["uv"]
        rotation = element.get("rotation", [0, 0, 0])
        cubes.append({
            "name": element["name"],
            "group": group_of(element["name"]),
            "glow": element["name"] in GLOW,
            "from": element["from"],
            "to": element["to"],
            "origin": element.get("origin", element["from"]),
            "rotation": rotation,
            "faces": faces,
        })

    model = {
        "uv_size": [data["resolution"]["width"], data["resolution"]["height"]],
        "frames": data["textures"][0]["height"] // data["textures"][0]["uv_height"],
        "frame_ticks": data["textures"][0]["frame_time"],
        "cubes": cubes,
    }
    (out / "models" / "grimorio.json").write_text(json.dumps(model, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
