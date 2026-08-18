import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { itemImageUrl } from '../api/item';
import { useAllCategorySubs, useCategoryMains, useCategorySubs } from '../hooks/useCategories';
import { useItemList } from '../hooks/useItems';

const PAGE_SIZE = 20;

/**
 * 쌍곰(ssangkom.co.kr) 제품 페이지의 대분류→중분류 연쇄 필터 + breadcrumb 패턴을
 * 참고했다. 그 사이트는 서버 렌더링이라 "적용하기" 버튼으로 페이지를 다시 불러오지만,
 * 여긴 SPA라 선택 즉시 반영한다(추가 클릭 없이 결과가 바로 좁혀짐).
 */
export default function ItemListPage() {
  const [page, setPage] = useState(0);
  const [mainId, setMainId] = useState<number | null>(null);
  const [subId, setSubId] = useState<number | null>(null);

  const mainsQuery = useCategoryMains();
  const subsQuery = useCategorySubs(mainId);
  const allSubsQuery = useAllCategorySubs();
  const itemsQuery = useItemList(page, PAGE_SIZE, mainId, subId);

  // 대분류명/중분류명을 미리 맵으로 만들어둔다 — "전체" 조회 결과의 각 행이 어느
  // 중분류든 이름으로 바로 찾을 수 있어야 한다(필터로 좁힌 목록만 표시하는 게 아니므로).
  const subNameById = useMemo(() => {
    const map = new Map<number, { name: string; categoryMainId: number }>();
    allSubsQuery.data?.forEach((s) => map.set(s.id, { name: s.name, categoryMainId: s.categoryMainId }));
    return map;
  }, [allSubsQuery.data]);

  const mainNameById = useMemo(() => {
    const map = new Map<number, string>();
    mainsQuery.data?.forEach((m) => map.set(m.id, m.name));
    return map;
  }, [mainsQuery.data]);

  function categoryLabel(categorySubId: number): string {
    const sub = subNameById.get(categorySubId);
    if (!sub) return '-';
    const mainName = mainNameById.get(sub.categoryMainId);
    return mainName ? `${mainName} > ${sub.name}` : sub.name;
  }

  function selectMain(next: number | null) {
    setMainId(next);
    setSubId(null);
    setPage(0);
  }

  function selectSub(next: number | null) {
    setSubId(next);
    setPage(0);
  }

  if (itemsQuery.isLoading) return <p>불러오는 중...</p>;
  if (itemsQuery.isError) return <p className="error-message">품목 목록을 불러오지 못했습니다.</p>;

  const data = itemsQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;
  const selectedMainName = mainId !== null ? mainNameById.get(mainId) : undefined;
  const selectedSubName = subId !== null ? subNameById.get(subId)?.name : undefined;

  return (
    <div className="page">
      <div className="page-header">
        <h1>품목 관리</h1>
        <Link to="/items/new">
          <button type="button">새 품목 등록</button>
        </Link>
      </div>

      <div className="category-filter">
        <ul className="category-tabs">
          <li>
            <label className={mainId === null ? 'tab-label active' : 'tab-label'}>
              <input type="radio" checked={mainId === null} onChange={() => selectMain(null)} />
              전체 품목
            </label>
          </li>
          {mainsQuery.data?.map((main) => (
            <li key={main.id}>
              <label className={mainId === main.id ? 'tab-label active' : 'tab-label'}>
                <input type="radio" checked={mainId === main.id} onChange={() => selectMain(main.id)} />
                {main.name}
              </label>
            </li>
          ))}
        </ul>

        {mainId !== null && (
          <ul className="category-pills">
            <li>
              <label className={subId === null ? 'pill-label active' : 'pill-label'}>
                <input type="radio" checked={subId === null} onChange={() => selectSub(null)} />
                전체
              </label>
            </li>
            {subsQuery.data?.map((sub) => (
              <li key={sub.id}>
                <label className={subId === sub.id ? 'pill-label active' : 'pill-label'}>
                  <input type="radio" checked={subId === sub.id} onChange={() => selectSub(sub.id)} />
                  {sub.name}
                </label>
              </li>
            ))}
          </ul>
        )}

        <div className="breadcrumb-bar">
          <span>
            {selectedMainName
              ? selectedSubName
                ? `${selectedMainName} > ${selectedSubName}`
                : selectedMainName
              : '전체 품목'}
          </span>
          {mainId !== null && (
            <button type="button" onClick={() => selectMain(null)}>
              분류 초기화
            </button>
          )}
        </div>
      </div>

      <ul className="item-grid">
        {data?.content.map((item) => (
          <li key={item.id} className="item-card">
            {/*
              참고 사이트처럼 품목명이 위, 사진이 아래에 온다. 목록 응답의 images에는
              대표 사진 1장만 담겨 오고(백엔드가 배치로 붙여준다), 사진이 없는 품목은
              첫 글자 플레이스홀더로 대신한다 — ERP라 사진 없는 품목이 정상적으로 있다.
            */}
            <div className="item-card-head">
              <h3>{item.name}</h3>
              <span className="item-card-category">{categoryLabel(item.categorySubId)}</span>
            </div>
            <Link to={`/items/${item.id}`} className="item-card-thumb">
              {item.images.length > 0 ? (
                <img src={itemImageUrl(item.id, item.images[0].id, 'thumb')} alt={item.name} />
              ) : (
                <span className="item-card-thumb-placeholder">{item.name.charAt(0)}</span>
              )}
            </Link>
            <div className="item-card-body">
              <span className="item-card-meta">KS규격: {item.ksStandard ?? '-'}</span>
              <span className="item-card-meta">
                인증정보: {item.certifications.map((c) => c.name).join(', ') || '-'}
              </span>
              <div className="item-card-footer">
                <span className={item.active ? 'badge badge-success' : 'badge badge-neutral'}>
                  {item.active ? '활성' : '비활성'}
                </span>
                <Link to={`/items/${item.id}`}>상세보기</Link>
              </div>
            </div>
          </li>
        ))}
      </ul>

      <div className="pagination">
        <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
          이전
        </button>
        <span>
          {page + 1} / {totalPages}
        </span>
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          다음
        </button>
      </div>
    </div>
  );
}
