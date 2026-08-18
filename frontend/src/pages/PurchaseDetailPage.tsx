import { useNavigate, useParams } from 'react-router-dom';
import { usePurchaseDetail, usePurchaseMutations } from '../hooks/usePurchases';
import type { PurchaseStatus } from '../types/api';

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

export default function PurchaseDetailPage() {
  const { id } = useParams<{ id: string }>();
  const purchaseId = Number(id);
  const navigate = useNavigate();

  const purchaseQuery = usePurchaseDetail(purchaseId);
  const { cancelMutation } = usePurchaseMutations();

  if (purchaseQuery.isLoading) return <p>불러오는 중...</p>;
  if (purchaseQuery.isError || !purchaseQuery.data) {
    return <p className="error-message">매입 전표를 찾을 수 없습니다.</p>;
  }

  const purchase = purchaseQuery.data;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{purchase.purchaseNo}</h1>
        <button type="button" onClick={() => navigate('/purchases')}>
          목록으로
        </button>
      </div>
      <p>매입일자: {purchase.purchaseDate}</p>
      <p>
        상태: <span className={STATUS_BADGE[purchase.status]}>{STATUS_LABEL[purchase.status]}</span>
      </p>
      <p>합계금액: {purchase.totalAmount.toLocaleString()}</p>
      <p>메모: {purchase.memo ?? '-'}</p>

      <h2>매입 품목</h2>
      <table>
        <thead>
          <tr>
            <th>규격 ID</th>
            <th>수량</th>
            <th>단가</th>
            <th>금액</th>
          </tr>
        </thead>
        <tbody>
          {purchase.items.map((item) => (
            <tr key={item.id}>
              <td>{item.itemSpecId}</td>
              <td>{item.quantity}</td>
              <td>{item.unitPrice.toLocaleString()}</td>
              <td>{item.amount.toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {purchase.status === 'CONFIRMED' && (
        <button
          type="button"
          className="button-danger"
          onClick={() => cancelMutation.mutate(purchase.id)}
          disabled={cancelMutation.isPending}
        >
          {cancelMutation.isPending ? '취소 처리 중...' : '매입 전표 취소'}
        </button>
      )}
      {cancelMutation.isError && (
        <p className="error-message">취소에 실패했습니다.</p>
      )}
    </div>
  );
}
