import { useState } from 'react';
import { Link } from 'react-router-dom';
import { usePurchaseList } from '../hooks/usePurchases';
import type { PurchaseStatus } from '../types/api';

const PAGE_SIZE = 20;

const STATUS_LABEL: Record<PurchaseStatus, string> = {
  DRAFT: '임시저장',
  CONFIRMED: '확정',
  CANCELED: '취소됨',
};

const STATUS_BADGE: Record<PurchaseStatus, string> = {
  DRAFT: 'badge badge-neutral',
  CONFIRMED: 'badge badge-success',
  CANCELED: 'badge badge-danger',
};

export default function PurchaseListPage() {
  const [page, setPage] = useState(0);
  const purchasesQuery = usePurchaseList(page, PAGE_SIZE);

  if (purchasesQuery.isLoading) return <p>불러오는 중...</p>;
  if (purchasesQuery.isError) return <p className="error-message">매입 전표 목록을 불러오지 못했습니다.</p>;

  const data = purchasesQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>매입 전표</h1>
        <Link to="/purchases/new">
          <button type="button">새 매입 전표 등록</button>
        </Link>
      </div>

      <table>
        <thead>
          <tr>
            <th>전표번호</th>
            <th>매입일자</th>
            <th>합계금액</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {data?.content.map((purchase) => (
            <tr key={purchase.id}>
              <td>{purchase.purchaseNo}</td>
              <td>{purchase.purchaseDate}</td>
              <td>{purchase.totalAmount.toLocaleString()}</td>
              <td>
                <span className={STATUS_BADGE[purchase.status]}>{STATUS_LABEL[purchase.status]}</span>
              </td>
              <td>
                <Link to={`/purchases/${purchase.id}`}>상세</Link>
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
