import copy
import json
import uuid


def load(path):
    data = json.load(open(path, encoding="utf-8"))
    table = {g["uuid"]: g for g in data.get("groups", [])}
    if table:
        def inline(nodes):
            out = []
            for node in nodes:
                if isinstance(node, dict):
                    merged = dict(table.get(node.get("uuid"), {}))
                    merged.update({k: v for k, v in node.items() if k != "children"})
                    merged["children"] = inline(node.get("children", []))
                    out.append(merged)
                else:
                    out.append(node)
            return out
        data["outliner"] = inline(data["outliner"])
        del data["groups"]
        data["meta"]["format_version"] = "4.10"
    return data


def save(data, path):
    with open(path, "w", encoding="utf-8") as out:
        json.dump(data, out, indent="\t", ensure_ascii=False)


def elements(data):
    return {e["uuid"]: e for e in data["elements"]}


def find_group(nodes, name):
    for node in nodes:
        if isinstance(node, dict):
            if node.get("name") == name:
                return node
            found = find_group(node.get("children", []), name)
            if found:
                return found
    return None


def groups(nodes):
    for node in nodes:
        if isinstance(node, dict):
            yield node
            yield from groups(node.get("children", []))


def subtree(node):
    cubes, nodes = [], [node]
    stack = [node]
    while stack:
        current = stack.pop()
        for child in current.get("children", []):
            if isinstance(child, str):
                cubes.append(child)
            else:
                nodes.append(child)
                stack.append(child)
    return cubes, nodes


def scale_point(p, pivot, factor):
    return [pivot[i] + (p[i] - pivot[i]) * factor[i] for i in range(3)]


def scale_subtree(data, node, pivot, factor):
    if not isinstance(factor, (list, tuple)):
        factor = [factor] * 3
    els = elements(data)
    cubes, nodes = subtree(node)
    for n in nodes:
        if "origin" in n:
            n["origin"] = scale_point(n["origin"], pivot, factor)
    for cid in cubes:
        e = els[cid]
        e["from"] = scale_point(e["from"], pivot, factor)
        e["to"] = scale_point(e["to"], pivot, factor)
        if "origin" in e:
            e["origin"] = scale_point(e["origin"], pivot, factor)
        for i in range(3):
            if e["from"][i] > e["to"][i]:
                e["from"][i], e["to"][i] = e["to"][i], e["from"][i]


def thicken(element, axes, factor):
    for i in axes:
        c = (element["from"][i] + element["to"][i]) / 2
        h = (element["to"][i] - element["from"][i]) / 2 * factor
        element["from"][i] = c - h
        element["to"][i] = c + h


def add_cube(data, parent, template, name, frm, to, faces_from=None):
    e = copy.deepcopy(template)
    e["uuid"] = str(uuid.uuid4())
    e["name"] = name
    e["from"] = list(frm)
    e["to"] = list(to)
    if faces_from is not None:
        e["faces"] = copy.deepcopy(faces_from["faces"])
    data["elements"].append(e)
    parent["children"].append(e["uuid"])
    return e


def parent_of(nodes, cid):
    for node in nodes:
        if isinstance(node, dict):
            if cid in node.get("children", []):
                return node
            found = parent_of(node.get("children", []), cid)
            if found:
                return found
    return None


def remove_cube(data, cid):
    parent = parent_of(data["outliner"], cid)
    if parent:
        parent["children"].remove(cid)
    elif cid in data["outliner"]:
        data["outliner"].remove(cid)
    data["elements"] = [e for e in data["elements"] if e["uuid"] != cid]
    for anim in data.get("animations", []):
        anim.get("animators", {}).pop(cid, None)
