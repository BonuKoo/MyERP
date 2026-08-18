import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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

      <fieldset>
        <legend>대분류</legend>
        <label>
          <input type="radio" checked={mainId === null} onChange={() => selectMain(null)} />
          전체
        </label>
        {mainsQuery.data?.map((main) => (
          <label key={main.id}>
            <input type="radio" checked={mainId === main.id} onChange={() => selectMain(main.id)} />
            {main.name}
          </label>
        ))}
      </fieldset>

      {mainId !== null && (
        <fieldset>
          <legend>중분류</legend>
          <label>
            <input type="radio" checked={subId === null} onChange={() => selectSub(null)} />
            전체
          </label>
          {subsQuery.data?.map((sub) => (
            <label key={sub.id}>
              <input type="radio" checked={subId === sub.id} onChange={() => selectSub(sub.id)} />
              {sub.name}
            </label>
          ))}
        </fieldset>
      )}

      <p className="breadcrumb">
        {selectedMainName ? (selectedSubName ? `${selectedMainName} > ${selectedSubName}` : selectedMainName) : '전체 품목'}
        {mainId !== null && (
          <button type="button" onClick={() => selectMain(null)}>
            분류 초기화
          </button>
        )}
      </p>

      <table>
        <thead>
          <tr>
            <th>품목명</th>
            <th>분류</th>
            <th>KS규격</th>
            <th>인증정보</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {data?.content.map((item) => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>{categoryLabel(item.categorySubId)}</td>
              <td>{item.ksStandard ?? '-'}</td>
              <td>{item.certifications.map((c) => c.name).join(', ') || '-'}</td>
              <td>{item.active ? '활성' : '비활성'}</td>
              <td>
                <Link to={`/items/${item.id}`}>상세</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

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
