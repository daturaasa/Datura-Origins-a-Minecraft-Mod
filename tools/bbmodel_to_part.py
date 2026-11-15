import base64
import json
import os
import sys

FACES = ("north", "south", "east", "west", "up", "down")
STATE_ANIMATIONS = {"swim", "swim_idle", "swim_fast", "crawl", "sit_down", "sit_idle", "fly", "rest"}
OPTIONS = {"hide_legs", "static", "pulse"}
INTERPOLATION = {"linear": 1, "step": 2}


def number(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def short(name):
    return name.rsplit(".", 1)[-1]


def convert(path, out_root, name=None, options=()):
    data = json.load(open(path, encoding="utf-8"))
    name = name or os.path.splitext(os.path.basename(path))[0]
    res = data.get("resolution", {})
    tex = data["textures"][0]
    width = tex.get("uv_width") or tex.get("width") or res.get("width", 16)
    height = tex.get("uv_height") or tex.get("height") or res.get("height", 16)
    elements = {e["uuid"]: e for e in data["elements"]}
    groups = {g["uuid"]: g for g in data.get("groups", [])}
    bones = []

    def cube(e):
        faces = {}
        for face in FACES:
            f = e["faces"].get(face)
            if f and f.get("texture") is not None and "uv" in f:
                faces[face] = list(f["uv"]) + [f.get("rotation", 0)]
        return {
            "from": e["from"],
            "to": e["to"],
            "origin": e.get("origin", [0, 0, 0]),
            "rotation": e.get("rotation", [0, 0, 0]),
            "inflate": e.get("inflate", 0),
            "faces": faces,
        }

    def walk(nodes, parent, glowing):
        loose = []
        for node in nodes:
            if isinstance(node, str):
                if node in elements:
                    loose.append(cube(elements[node]))
                continue
            if "name" not in node and node.get("uuid") in groups:
                node = dict(groups[node["uuid"]], children=node.get("children", []))
            glow = glowing or node["name"].endswith("_glow")
            bone = {
                "name": node["name"],
                "parent": parent,
                "origin": node.get("origin", [0, 0, 0]),
                "rotation": node.get("rotation", [0, 0, 0]),
                "cubes": [],
            }
            if glow:
                bone["glow"] = True
            bones.append(bone)
            bone["cubes"] = walk(node.get("children", []), node["name"], glow)
        return loose

    root_cubes = walk(data["outliner"], None, False)
    if root_cubes:
        bones.insert(0, {"name": "root", "parent": None, "origin": [0, 0, 0], "rotation": [0, 0, 0], "cubes": root_cubes})

    lift = next((b["name"] for b in bones if b["cubes"]), bones[0]["name"])

    animations = {}
    for anim in data.get("animations", []):
        tracks = {}
        for animator in anim.get("animators", {}).values():
            merged = {}
            for kf in animator.get("keyframes", []):
                p = kf["data_points"][0]
                x, y, z = number(p.get("x")), number(p.get("y")), number(p.get("z"))
                channel = kf["channel"]
                if channel == "rotation":
                    value = [-x, -y, z]
                elif channel == "position":
                    value = [-x, y, z]
                elif channel == "scale":
                    value = [x, y, z]
                else:
                    continue
                mode = INTERPOLATION.get(kf.get("interpolation"), 0)
                key = (channel, round(number(kf["time"]), 4))
                if key in merged:
                    old = merged[key]
                    combined = [a * b for a, b in zip(old[0], value)] if channel == "scale" else [a + b for a, b in zip(old[0], value)]
                    merged[key] = (combined, old[1])
                else:
                    merged[key] = (value, mode)
            channels = {}
            for (channel, time), (value, mode) in sorted(merged.items(), key=lambda item: item[0][1]):
                channels.setdefault(channel, []).append([time] + value + [mode])
            if channels:
                tracks[animator["name"]] = channels
        animations[short(anim["name"])] = {
            "length": number(anim.get("length", 1)) or 1,
            "loop": anim.get("loop") == "loop",
            "bones": tracks,
        }

    roots = {b["name"] for b in bones if b["parent"] is None}
    lowest = min((c["from"][1] for b in bones for c in b["cubes"]), default=0)
    if "head" in roots:
        attach = "head"
    elif "body" in roots:
        attach = "body"
    else:
        attach = "head" if lowest >= 24 else "body"
    physics = attach == "body" and "static" not in options and not (STATE_ANIMATIONS & set(animations))
    model = {"texture_size": [width, height], "attach": attach, "physics": physics, "lift_bone": lift,
             "bones": bones, "animations": animations}
    if "hide_legs" in options:
        model["hide"] = ["legs"]
    if "pulse" in options:
        model["pulse"] = True
    models_dir = os.path.join(out_root, "assets", "datura_origins", "models", "part")
    textures_dir = os.path.join(out_root, "assets", "datura_origins", "textures", "entity", "part")
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(textures_dir, exist_ok=True)
    with open(os.path.join(models_dir, name + ".json"), "w", encoding="utf-8") as out:
        json.dump(model, out, separators=(",", ":"))
    source = tex["source"].split(",", 1)[1]
    with open(os.path.join(textures_dir, name + ".png"), "wb") as out:
        out.write(base64.b64decode(source))
    print(name, len(bones), "bones", sum(len(b["cubes"]) for b in bones), "cubes", sorted(animations),
          "attach:", attach, "physics:", physics)


if __name__ == "__main__":
    out_root = sys.argv[1]
    for arg in sys.argv[2:]:
        src, _, target = arg.partition("=")
        target, *opts = target.split(":")
        unknown = set(opts) - OPTIONS
        if unknown:
            sys.exit("unknown options: " + ", ".join(sorted(unknown)))
        convert(src, out_root, target or None, opts)
