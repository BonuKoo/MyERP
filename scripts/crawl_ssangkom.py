"""
ssangkom.co.kr 제품 카탈로그 수집기 (myerp 시드용).

구조는 브라우저로 직접 확인한 것을 그대로 옮겼다:
  - 목록: /product/product.php?cate={대분류}&scate={중분류}
    대분류/중분류 코드는 목록 페이지의 radio input(value)에 들어 있다.
    제품 카드는 detail.php 링크이고 제품명은 img[alt]에 있다.
  - 상세: .explanation(설명), .certify li(인증), tr의 th/td(용 량/주성분/유효기간)
    제품 이미지는 <img src>가 아니라 카카오톡 공유 버튼 설정(Kakao.Share의
    imageUrl)에 있다 - <img>의 src는 지연로딩용 base64 placeholder라 못 쓴다.
    /data/goods/ 경로의 첫 매치가 이 공유용 썸네일이고, 두 번째 이후 매치는
    상세 설명 전체가 통짜 이미지로 들어간 인포그래픽(세로 수만 px, 10MB급)이라
    쓰면 안 된다 - 실제로 이 실수로 108개 중 52개가 5MB 업로드 제한을 넘겼었다.

서버 부담을 줄이려고 요청마다 간격을 둔다. 결과물(catalog.json, images/)은
.gitignore에 있어 커밋되지 않는다 - 재실행하면 다시 만들 수 있고, 크롤링한
텍스트/이미지는 원 사이트의 저작물이라 리포에 영구 보관하지 않는다.
"""
import json
import re
import time
import urllib.request
from pathlib import Path

import lxml.html

BASE = "https://ssangkom.co.kr"
DELAY = 0.15
OUT_DIR = Path(__file__).parent
IMG_DIR = OUT_DIR / "images"
GOODS_RE = re.compile(r"/data/goods/[A-Za-z0-9._-]+")
KS_RE = re.compile(r"KS\s*[A-Z]?\s*[A-Z]\s*\d{3,4}|KSL\d{3,4}")

UA = {"User-Agent": "Mozilla/5.0 (myerp seed crawler; local portfolio project)"}


def get(path):
    req = urllib.request.Request(BASE + path, headers=UA)
    with urllib.request.urlopen(req, timeout=20) as r:
        return r.read()


def clean(s):
    return re.sub(r"\s+", " ", (s or "")).strip()


def collect_categories():
    doc = lxml.html.fromstring(get("/product/product.php?cate=100"))
    mains, subs = [], []
    for inp in doc.xpath('//input[@name="menu-item"]'):
        code = inp.get("value")
        if not code:
            continue
        li = inp.getparent()
        while li is not None and li.tag != "li":
            li = li.getparent()
        mains.append({"code": code, "name": clean(li.text_content() if li is not None else "")})
    for inp in doc.xpath('//input[@name="sub-menu-item"]'):
        code = inp.get("value")
        if not code or not code.isdigit():
            continue
        li = inp.getparent()
        while li is not None and li.tag != "li":
            li = li.getparent()
        subs.append({"code": code, "name": clean(li.text_content() if li is not None else "")})
    return mains, subs


def collect_products(mains, subs):
    products = []
    for sub in subs:
        main_code = sub["code"][:3]
        main = next((m for m in mains if m["code"] == main_code), None)
        doc = lxml.html.fromstring(get(f"/product/product.php?cate={main_code}&scate={sub['code']}"))
        for a in doc.xpath('//a[contains(@href,"detail.php")]'):
            imgs = a.xpath(".//img")
            name = clean(imgs[0].get("alt")) if imgs else ""
            if not name:
                continue
            products.append({
                "mainCode": main_code,
                "mainName": main["name"] if main else None,
                "subCode": sub["code"],
                "subName": sub["name"],
                "href": a.get("href"),
                "name": name,
            })
        time.sleep(DELAY)
    return products


def collect_detail(p):
    href = p["href"].lstrip("./")
    raw = get("/product/" + href)
    doc = lxml.html.fromstring(raw)

    explanation = clean(" ".join(doc.xpath('//*[contains(@class,"explanation")]//text()')))
    # li 안에 <span> 등이 중첩돼 있어서 //text()로 모으면 "친환경 (", "HB", ": 최우수)..."
    # 처럼 조각난다. li 단위로 text_content()를 써야 한 항목으로 합쳐진다.
    certify = []
    for li in doc.xpath('//*[contains(@class,"certify")]//li'):
        t = clean(li.text_content())
        if t:
            certify.append(t)

    spec = {}
    for tr in doc.xpath("//tr"):
        ths, tds = tr.xpath("./th"), tr.xpath("./td")
        if ths and tds:
            key = re.sub(r"\s", "", clean(ths[0].text_content()))
            spec[key] = clean(tds[0].text_content())

    # 이미지는 <img src>가 아니라 카카오톡 공유 버튼 설정(Kakao.Share imageUrl)에
    # 실제 경로가 있다 — <img>의 src는 지연로딩용 base64 placeholder라 못 쓴다.
    # 원문 정규식으로 /data/goods/ 첫 매치를 쓰면 이 공유용 썸네일이 잡힌다.
    # (두 번째 이후 매치는 상세 설명 전체가 통짜 이미지로 들어간 인포그래픽이라
    #  세로 수만 px짜리 10MB급 파일이다 — 첫 제품에서 실제로 확인함. 절대 쓰지 않는다.)
    raw_text = raw.decode("utf-8", "replace")
    m = GOODS_RE.search(raw_text)
    img = m.group(0) if m else None

    # "KSL1593"과 "KS L 1593"이 섞여 있어 표기를 통일한다.
    ks_raw = KS_RE.search(explanation or "")
    ks = None
    if ks_raw:
        compact = re.sub(r"\s+", "", ks_raw.group(0))
        m = re.match(r"KS([A-Z])(\d{3,4})", compact)
        ks = f"KS {m.group(1)} {m.group(2)}" if m else clean(ks_raw.group(0))

    return {
        "mainName": p["mainName"], "subName": p["subName"], "name": p["name"],
        "explanation": explanation or None,
        "certify": certify,
        "capacity": spec.get("용량"),
        "ingredient": spec.get("주성분"),
        "shelfLife": spec.get("유효기간"),
        "ks": ks,
        "img": img,
    }


def download_images(items):
    IMG_DIR.mkdir(exist_ok=True)
    ok = 0
    for it in items:
        if not it.get("img"):
            continue
        fname = it["img"].rsplit("/", 1)[-1]
        dest = IMG_DIR / fname
        it["imgFile"] = str(dest)
        if dest.exists():
            ok += 1
            continue
        try:
            with open(dest, "wb") as f:
                f.write(get(it["img"]))
            ok += 1
        except Exception as e:
            print(f"  이미지 실패 {it['name']}: {e}")
            it["imgFile"] = None
        time.sleep(DELAY)
    return ok


def main():
    mains, subs = collect_categories()
    print(f"대분류 {len(mains)} / 중분류 {len(subs)}")

    products = collect_products(mains, subs)
    print(f"제품 {len(products)}개 (고유명 {len({p['name'] for p in products})})")

    details = []
    for i, p in enumerate(products, 1):
        try:
            details.append(collect_detail(p))
        except Exception as e:
            print(f"  상세 실패 {p['name']}: {e}")
        if i % 25 == 0:
            print(f"  상세 {i}/{len(products)}")
        time.sleep(DELAY)

    n_img = download_images(details)

    (OUT_DIR / "catalog.json").write_text(
        json.dumps(details, ensure_ascii=False, indent=1), encoding="utf-8")

    print("---")
    print(f"수집 {len(details)} / 설명 {sum(1 for d in details if d['explanation'])}"
          f" / 용량 {sum(1 for d in details if d['capacity'])}"
          f" / 인증 {sum(1 for d in details if d['certify'])}"
          f" / 이미지파일 {n_img}")
    print(f"저장: {OUT_DIR / 'catalog.json'}")


if __name__ == "__main__":
    main()
