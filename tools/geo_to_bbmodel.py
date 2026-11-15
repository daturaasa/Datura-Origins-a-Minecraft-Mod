import base64
import json
import sys
import uuid


def faces(u, v, w, h, d):
    return {
        "north": {"uv": [u + d, v + d, u + d + w, v + d + h], "texture": 0},
        "east": {"uv": [u, v + d, u + d, v + d + h], "texture": 0},
        "south": {"uv": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], "texture": 0},
        "west": {"uv": [u + d + w, v + d, u + 2 * d + w, v + d + h], "texture": 0},
        "up": {"uv": [u + d + w, v + d, u + d, v], "texture": 0},
        "down": {"uv": [u + d + 2 * w, v, u + d + w, v + d], "texture": 0},
    }


def convert(geo_path, texture_path, out_path, name):
    geo = json.load(open(geo_path))["minecraft:geometry"][0]
    desc = geo["description"]
    tw, th = desc.get("texture_width", 64), desc.get("texture_height", 64)
    elements, nodes = [], {}
    outliner = []
    for bone in geo["bones"]:
        pivot = bone.get("pivot", [0, 0, 0])
        rot = bone.get("rotation", [0, 0, 0])
        node = {
            "name": bone["name"],
            "origin": [-pivot[0], pivot[1], pivot[2]],
            "rotation": [-rot[0], -rot[1], rot[2]],
            "uuid": str(uuid.uuid4()),
            "export": True,
            "isOpen": True,
            "visibility": True,
            "children": [],
        }
        for i, c in enumerate(bone.get("cubes", [])):
            o, s = c["origin"], c["size"]
            u, v = c.get("uv", [0, 0])
            cp = c.get("pivot", pivot)
            cr = c.get("rotation", [0, 0, 0])
            element = {
                "name": f"{bone['name']}_{i}",
                "box_uv": False,
                "rescale": False,
                "from": [-(o[0] + s[0]), o[1], o[2]],
                "to": [-o[0], o[1] + s[1], o[2] + s[2]],
                "origin": [-cp[0], cp[1], cp[2]],
                "rotation": [-cr[0], -cr[1], cr[2]],
                "inflate": c.get("inflate", 0),
                "faces": faces(u, v, s[0], s[1], s[2]),
                "type": "cube",
                "uuid": str(uuid.uuid4()),
            }
            elements.append(element)
            node["children"].append(element["uuid"])
        nodes[bone["name"]] = node
        parent = bone.get("parent")
        (nodes[parent]["children"] if parent in nodes else outliner).append(node)
    source = "data:image/png;base64," + base64.b64encode(open(texture_path, "rb").read()).decode()
    data = {
        "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
        "name": name,
        "resolution": {"width": tw, "height": th},
        "elements": elements,
        "outliner": outliner,
        "textures": [{"name": name + ".png", "width": tw, "height": th, "uv_width": tw, "uv_height": th,
                      "id": "0", "uuid": str(uuid.uuid4()), "source": source}],
        "animations": [],
    }
    json.dump(data, open(out_path, "w", encoding="utf-8"), indent="\t")


if __name__ == "__main__":
    convert(*sys.argv[1:5])
