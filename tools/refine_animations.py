import copy
import json
import math
import sys
import uuid

LOOPED_BASE = ("swim", "swim_idle", "swim_fast", "crawl", "sit_idle", "idle", "fly", "rest", "tremer", "walk")


def short(name):
    return name.rsplit(".", 1)[-1]


def number(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.0


def keyframe(channel, time, x, y, z):
    return {
        "channel": channel,
        "data_points": [{"x": round(x, 3), "y": round(y, 3), "z": round(z, 3)}],
        "uuid": str(uuid.uuid4()),
        "time": round(time, 4),
        "color": -1,
        "interpolation": "catmullrom",
    }


def channels(animator):
    merged = {}
    for kf in animator.get("keyframes", []):
        p = kf["data_points"][0]
        key = (kf["channel"], round(number(kf["time"]), 4))
        value = [number(p.get("x")), number(p.get("y")), number(p.get("z"))]
        if key in merged:
            old = merged[key]
            merged[key] = [a * b for a, b in zip(old, value)] if kf["channel"] == "scale" else [a + b for a, b in zip(old, value)]
        else:
            merged[key] = value
    out = {}
    for (channel, time), value in sorted(merged.items(), key=lambda item: item[0][1]):
        out.setdefault(channel, []).append([time] + value)
    return out


def rebuild(animator, chans):
    animator["keyframes"] = [keyframe(ch, *f) for ch, frames in chans.items() for f in frames]


def seamless(frames, length):
    if not frames:
        return frames
    frames = [f for f in frames if f[0] < length - 1e-4]
    if not frames or frames[0][0] > 1e-4:
        first = frames[0] if frames else [0, 0, 0, 0]
        frames.insert(0, [0.0] + first[1:])
    frames.append([length] + frames[0][1:])
    return frames


def smooth_cyclic(frames):
    body = frames[:-1]
    n = len(body)
    if n < 4:
        return frames
    out = []
    for i in range(n):
        a, b, c = body[i - 1], body[i], body[(i + 1) % n]
        out.append([b[0]] + [(a[k] + 2 * b[k] + c[k]) / 4 for k in (1, 2, 3)])
    out.append([frames[-1][0]] + out[0][1:])
    return out


def refine_loops(data):
    for anim in data.get("animations", []):
        if anim.get("loop") != "loop":
            continue
        length = number(anim.get("length", 1))
        for animator in anim["animators"].values():
            chans = channels(animator)
            for ch in list(chans):
                frames = seamless(chans[ch], length)
                chans[ch] = smooth_cyclic(frames) if ch == "rotation" else frames
            rebuild(animator, chans)


def ease(t):
    return t * t * t * (t * (t * 6 - 15) + 10)


def ease_once(data, name, steps=7):
    for anim in data.get("animations", []):
        if short(anim["name"]) != name:
            continue
        length = number(anim.get("length", 1))
        for animator in anim["animators"].values():
            chans = channels(animator)
            for ch, frames in chans.items():
                start, end = frames[0], frames[-1]
                chans[ch] = [[length * i / steps] + [start[k] + (end[k] - start[k]) * ease(i / steps) for k in (1, 2, 3)]
                             for i in range(steps + 1)]
            rebuild(animator, chans)


def flip_pitch(data, names):
    for anim in data.get("animations", []):
        if short(anim["name"]) not in names:
            continue
        for animator in anim["animators"].values():
            chans = channels(animator)
            if "rotation" in chans:
                chans["rotation"] = [[f[0], -f[1], f[2], f[3]] for f in chans["rotation"]]
            rebuild(animator, chans)


def add_fast_swim(data):
    names = {short(a["name"]) for a in data.get("animations", [])}
    if "swim_fast" in names or "swim" not in names:
        return
    swim = next(a for a in data["animations"] if short(a["name"]) == "swim")
    fast = copy.deepcopy(swim)
    fast["uuid"] = str(uuid.uuid4())
    fast["name"] = swim["name"].replace("swim", "swim_fast")
    fast["length"] = round(number(swim["length"]) * 0.55, 4)
    for animator in fast["animators"].values():
        chans = channels(animator)
        for ch, frames in chans.items():
            gain = 1.25 if ch == "rotation" else 1.0
            chans[ch] = [[f[0] * 0.55] + [v * gain for v in f[1:]] for f in frames]
        rebuild(animator, chans)
    data["animations"].append(fast)


def bone_ids(data):
    ids = {}

    def walk(nodes):
        for node in nodes:
            if isinstance(node, str):
                continue
            ids[node.get("name")] = node["uuid"]
            walk(node.get("children", []))

    walk(data["outliner"])
    for group in data.get("groups", []):
        ids[group["name"]] = group["uuid"]
    return ids


def wing_animation(data, name, length, frames_by_bone):
    ids = bone_ids(data)
    animators = {}
    for bone, frames in frames_by_bone.items():
        animators[ids[bone]] = {"name": bone, "type": "bone",
                                "keyframes": [keyframe("rotation", t, x, y, z) for t, x, y, z in frames]}
    data["animations"] = [a for a in data.get("animations", []) if a["name"] != name]
    data["animations"].append({
        "uuid": str(uuid.uuid4()), "name": name, "loop": "loop", "override": False, "length": length,
        "snapping": 48, "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "",
        "loop_delay": "", "animators": animators})


def flap(length, count, sweep, lift, lag, mirror):
    out = []
    for i in range(count + 1):
        t = length * i / count
        phase = 2 * math.pi * (i / count) - lag
        swing = 0.5 - 0.5 * math.cos(phase)
        y = (-6 + sweep * swing) * mirror
        z = lift * math.sin(phase) * mirror
        x = 4 * math.sin(phase)
        out.append((t, x, y, z))
    return out


def hang(length, count, fold, droop, sway, mirror):
    out = []
    for i in range(count + 1):
        t = length * i / count
        phase = 2 * math.pi * i / count
        out.append((t, sway * math.sin(phase), (fold + sway * 0.6 * math.cos(phase)) * mirror, (-droop + sway * math.sin(phase)) * mirror))
    return out


def moth_wings(data):
    tremer = next((a for a in data.get("animations", []) if a["name"] == "tremer"), None)
    if tremer is not None and not any(a["name"] == "idle" for a in data["animations"]):
        idle = copy.deepcopy(tremer)
        idle["uuid"] = str(uuid.uuid4())
        idle["name"] = "idle"
        data["animations"].append(idle)
    wing_animation(data, "fly", 0.2, {
        "asa_r_frente": flap(0.2, 8, 62, 10, 0.0, 1),
        "asa_r_tras": flap(0.2, 8, 52, 8, 0.55, 1),
        "asa_l_frente": flap(0.2, 8, 62, 10, 0.0, -1),
        "asa_l_tras": flap(0.2, 8, 52, 8, 0.55, -1),
    })
    wing_animation(data, "rest", 3.0, {
        "asa_r_frente": hang(3.0, 6, 34, 72, 1.5, 1),
        "asa_r_tras": hang(3.0, 6, 28, 30, 1.2, 1),
        "asa_l_frente": hang(3.0, 6, 34, 72, 1.5, -1),
        "asa_l_tras": hang(3.0, 6, 28, 30, 1.2, -1),
    })


def mermaid(data, flip_sit):
    if flip_sit:
        flip_pitch(data, {"sit_down", "sit_idle"})
    ease_once(data, "sit_down")
    add_fast_swim(data)
    refine_loops(data)
    for anim in data.get("animations", []):
        if short(anim["name"]) == "sit_down":
            anim["loop"] = "hold"


def rename(data, names):
    for anim in data.get("animations", []):
        if anim["name"] in names:
            anim["name"] = names[anim["name"]]


def copy_animation(data, source, target):
    anim = next((a for a in data.get("animations", []) if a["name"] == source), None)
    if anim is None or any(a["name"] == target for a in data["animations"]):
        return
    dup = copy.deepcopy(anim)
    dup["uuid"] = str(uuid.uuid4())
    dup["name"] = target
    data["animations"].append(dup)


def normalize(data):
    for anim in data.get("animations", []):
        for animator in anim["animators"].values():
            rebuild(animator, channels(animator))


def gargoyle_wings(data):
    normalize(data)
    copy_animation(data, "wing_fly", "fly")
    copy_animation(data, "wing_glide", "glide")
    copy_animation(data, "wing_idle", "idle")
    refine_loops(data)


def kitsune_tails(data):
    rename(data, {"parado": "idle", "correr": "walk"})
    normalize(data)
    refine_loops(data)


def ear_animation(data, name, length, frames_by_bone):
    wing_animation(data, name, length, frames_by_bone)


def fox_ears(data):
    idle_r = [(0, 0, 0, 0), (1.2, 2, 0, -2), (2.4, 0, 0, 0), (2.6, -9, 0, 8), (2.75, 3, 0, -3), (2.9, 0, 0, 0), (4.0, 0, 0, 0)]
    idle_l = [(0, 0, 0, 0), (0.8, 1.5, 0, 1.5), (1.6, 0, 0, 0), (3.2, 2, 0, 2), (4.0, 0, 0, 0)]
    walk_r = [(0, 4, 0, -2), (0.25, -3, 0, 3), (0.5, 4, 0, -2)]
    walk_l = [(0, -3, 0, -3), (0.25, 4, 0, 2), (0.5, -3, 0, -3)]
    ear_animation(data, "idle", 4.0, {"orelha_r": idle_r, "orelha_l": idle_l})
    ear_animation(data, "walk", 0.5, {"orelha_r": walk_r, "orelha_l": walk_l})


def recolor(data, palette):
    import base64
    import io
    from PIL import Image
    tex = data["textures"][0]
    head, _, payload = tex["source"].partition(",")
    image = Image.open(io.BytesIO(base64.b64decode(payload))).convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            light = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            pixels[x, y] = tuple(int(round(c)) for c in palette(light)) + (a,)
    out = io.BytesIO()
    image.save(out, format="PNG")
    tex["source"] = head + "," + base64.b64encode(out.getvalue()).decode()


def blood_red(light):
    stops = [(0.0, (18, 0, 4)), (0.35, (70, 4, 14)), (0.65, (122, 10, 26)), (0.88, (168, 24, 38)), (1.0, (214, 82, 70))]
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if light <= b:
            t = (light - a) / (b - a)
            return [ca[i] + (cb[i] - ca[i]) * t for i in range(3)]
    return stops[-1][1]


def main():
    kind, src, dst = sys.argv[1], sys.argv[2], sys.argv[3]
    data = json.load(open(src, encoding="utf-8"))
    if kind == "wings":
        moth_wings(data)
    elif kind == "mermaid":
        mermaid(data, False)
    elif kind == "mermaid_flip":
        mermaid(data, True)
    elif kind == "gargoyle":
        gargoyle_wings(data)
    elif kind == "tails":
        kitsune_tails(data)
    elif kind == "tails_dark":
        kitsune_tails(data)
        recolor(data, blood_red)
    elif kind == "ears":
        fox_ears(data)
    elif kind == "ears_dark":
        fox_ears(data)
        recolor(data, blood_red)
    json.dump(data, open(dst, "w", encoding="utf-8"), ensure_ascii=False)


if __name__ == "__main__":
    main()
