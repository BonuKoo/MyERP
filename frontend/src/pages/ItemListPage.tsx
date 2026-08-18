import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useItemList } from '../hooks/useItems';

const PAGE_SIZE = 20;

export default function ItemListPage() {
  const [page, setPage] = useState(0);
  const itemsQuery = useItemList(page, PAGE_SIZE);

  if (itemsQuery.isLoading) return <p>불러오는 중...</p>;
  if (itemsQuery.isError) return <p className="error-message">품목 목록을 불러오지 못했습니다.</p>;

  const data = itemsQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>품목 관리</h1>
        <Link to="/items/new">
          <button type="button">새 품목 등록</button>
        </Link>
      </div>

      <table>
        <thead>
          <tr>
            <th>품목명</th>
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
