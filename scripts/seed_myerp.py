"""
catalog.json(ssangkom 크롤링 결과)을 myerp 실제 API로 적재한다.

순서 (전부 이미 있는 API만 사용, 새로 만드는 엔드포인트 없음):
  1. OWNER 로그인
  2. GET /api/categories/sub 로 대분류/중분류 -> categorySubId 매핑 확보
     (중분류명이 애매하면 대분류명까지 같이 맞춰서 구분한다 - 중분류명이
     대분류를 넘어 중복될 수 있어서)
  3. GET /api/certifications 로 기존 인증정보 확인, 카탈로그에서 나온
     인증 항목 중 없는 것만 POST /api/certifications 로 생성(OWNER 전용,
     실제 108건 적재 시 9종)
  4. 각 제품마다:
     a. POST /api/items (설명=explanation, ksStandard=ks, certificationIds)
     b. capacity가 있으면 POST /api/items/{id}/specs
        - 가격 정보가 사이트에 없어 원가/판매가는 1원 더미값(사용자 확인됨).
          @DecimalMin("0.01") 검증 때문에 0은 불가.
        - specName은 원문 capacity를 그대로 쓰되 DB item_spec.spec_name이
          VARCHAR(50)이라 50자에 맞춰 자른다
     c. img가 있으면 POST /api/items/{id}/images (multipart)

실패는 죽지 않고 기록만 남기고 계속 진행한다 - 108개 중 하나가 실패했다고
전체를 멈추면 나머지 성공분까지 다시 확인해야 해서 비효율적이다.
"""
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

BASE = "http://localhost:8080"
SCRATCH = Path(__file__).parent
IMG_DIR = SCRATCH / "images"


def call(method, path, token=None, json_body=None, files=None):
    url = BASE + path
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    if files is not None:
        boundary = uuid.uuid4().hex
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
        body = bytearray()
        for field_name, (filename, content, content_type) in files.items():
            body += f"--{boundary}\r\n".encode()
            body += (f'Content-Disposition: form-data; name="{field_name}"; '
                      f'filename="{filename}"\r\n').encode()
            body += f"Content-Type: {content_type}\r\n\r\n".encode()
            body += content
            body += b"\r\n"
        body += f"--{boundary}--\r\n".encode()
        data = bytes(body)
    elif json_body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(json_body, ensure_ascii=False).encode("utf-8")
    else:
        data = None

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            body = r.read()
            return r.status, (json.loads(body) if body else None)
    except urllib.error.HTTPError as e:
        body = e.read()
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, body.decode("utf-8", "replace")


def login():
    status, res = call("POST", "/api/auth/login", json_body={
        "email": "owner@myerp.com", "password": "password123"})
    if status != 200:
        raise RuntimeError(f"로그인 실패: {status} {res}")
    return res["accessToken"]


def build_category_map(token):
    status, subs = call("GET", "/api/categories/sub", token=token)
    if status != 200:
        raise RuntimeError(f"중분류 조회 실패: {status} {subs}")
    status, mains = call("GET", "/api/categories/main", token=token)
    main_by_id = {m["id"]: m["name"] for m in mains}
    # (대분류명, 중분류명) -> categorySubId. 중분류명이 유일하지 않을 수 있어
    # 대분류명까지 키에 포함한다.
    m = {}
    for s in subs:
        key = (main_by_id.get(s["categoryMainId"]), s["name"])
        m[key] = s["id"]
    return m


def normalize_sub_name(name):
    # DB 시드 스크립트(02_seed_category.sql)와 현재 사이트 표기가 구두점만
    # 다른 경우가 있다(확인된 예: "자동수평몰탈·특수몰탈" vs "자동수평몰탈,특수몰탈").
    return re.sub(r"[·,\s]", "", name or "")


def resolve_category(cat_map, main_name, sub_name):
    key = (main_name, sub_name)
    if key in cat_map:
        return cat_map[key]
    target = (main_name, normalize_sub_name(sub_name))
    for (m, s), sub_id in cat_map.items():
        if (m, normalize_sub_name(s)) == target:
            return sub_id
    return None


def build_cert_map(token, needed_names):
    status, existing = call("GET", "/api/certifications", token=token)
    if status != 200:
        raise RuntimeError(f"인증정보 조회 실패: {status} {existing}")
    m = {c["name"]: c["id"] for c in existing}
    for name in needed_names:
        if name in m:
            continue
        status, created = call("POST", "/api/certifications", token=token,
                                json_body={"name": name})
        if status == 201:
            m[name] = created["id"]
            print(f"  인증정보 생성: {name}")
        elif status == 409:
            # 동시에 다른 곳에서 만들어졌을 수 있으니 재조회
            status2, existing2 = call("GET", "/api/certifications", token=token)
            m = {c["name"]: c["id"] for c in existing2}
        else:
            print(f"  인증정보 생성 실패({status}): {name} -> {created}")
    return m


def guess_content_type(filename):
    ext = filename.rsplit(".", 1)[-1].lower()
    return {"jpg": "image/jpeg", "jpeg": "image/jpeg", "png": "image/png",
            "gif": "image/gif"}.get(ext, "application/octet-stream")


def main():
    catalog = json.loads((SCRATCH / "catalog.json").read_text(encoding="utf-8"))
    print(f"카탈로그 {len(catalog)}건 로드")

    token = login()
    print("OWNER 로그인 완료")

    cat_map = build_category_map(token)
    print(f"중분류 매핑 {len(cat_map)}건")

    cert_names = sorted({c for p in catalog for c in p["certify"]})
    cert_map = build_cert_map(token, cert_names)
    print(f"인증정보 매핑 {len(cert_map)}종")

    results = {"item_ok": 0, "item_fail": [], "spec_ok": 0, "spec_fail": [],
               "img_ok": 0, "img_fail": [], "category_miss": []}

    for i, p in enumerate(catalog, 1):
        sub_id = resolve_category(cat_map, p["mainName"], p["subName"])
        if sub_id is None:
            results["category_miss"].append(f"{p['name']} ({p['mainName']}, {p['subName']})")
            continue

        cert_ids = [cert_map[c] for c in p["certify"] if c in cert_map]
        req = {
            "categorySubId": sub_id,
            "name": p["name"],
            "description": (p["explanation"] or "")[:2000] or None,
            "ksStandard": p["ks"],
            "certificationIds": cert_ids,
        }
        status, created = call("POST", "/api/items", token=token, json_body=req)
        if status != 201:
            results["item_fail"].append(f"{p['name']}: {status} {created}")
            continue
        results["item_ok"] += 1
        item_id = created["id"]

        if p["capacity"]:
            spec_req = {
                "specName": p["capacity"][:50],
                "unit": "EA",
                "costPrice": 1,
                "salePrice": 1,
                "safetyStock": 0,
            }
            s_status, s_res = call("POST", f"/api/items/{item_id}/specs",
                                    token=token, json_body=spec_req)
            if s_status == 201:
                results["spec_ok"] += 1
            else:
                results["spec_fail"].append(f"{p['name']}: {s_status} {s_res}")

        img_file = IMG_DIR / (p["img"].rsplit("/", 1)[-1] if p["img"] else "")
        if p["img"] and img_file.exists():
            content = img_file.read_bytes()
            files = {"files": (img_file.name, content, guess_content_type(img_file.name))}
            i_status, i_res = call("POST", f"/api/items/{item_id}/images",
                                    token=token, files=files)
            if i_status == 201:
                results["img_ok"] += 1
            else:
                results["img_fail"].append(f"{p['name']}: {i_status} {i_res}")

        if i % 20 == 0:
            print(f"  진행 {i}/{len(catalog)}")
        time.sleep(0.03)

    print("---")
    print(f"품목 등록 {results['item_ok']}/{len(catalog)} (실패 {len(results['item_fail'])})")
    print(f"규격 등록 {results['spec_ok']} (실패 {len(results['spec_fail'])})")
    print(f"사진 등록 {results['img_ok']} (실패 {len(results['img_fail'])})")
    if results["category_miss"]:
        print(f"카테고리 매핑 실패 {len(results['category_miss'])}건:")
        for x in results["category_miss"][:10]:
            print("  ", x)
    for label, arr in [("품목", results["item_fail"]), ("규격", results["spec_fail"]),
                        ("사진", results["img_fail"])]:
        if arr:
            print(f"{label} 실패 상세(최대 5):")
            for x in arr[:5]:
                print("  ", x)

    (SCRATCH / "seed_result.json").write_text(
        json.dumps(results, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main()
