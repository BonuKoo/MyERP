import { useNavigate, useParams } from 'react-router-dom';
import { useSaleDetail, useSaleMutations } from '../hooks/useSales';
import { useAuth } from '../auth/AuthContext';
import { errorMessageOf } from '../api/client';
import type { SaleStatus } from '../types/api';

const STATUS_LABEL: Record<SaleStatus, string> = {
  DRAFT: '임시저장',
  CONFIRMED: '확정',
  CANCELED: '취소됨',
};

const STATUS_BADGE: Record<SaleStatus, string> = {
  DRAFT: 'badge badge-neutral',
  CONFIRMED: 'badge badge-success',
  CANCELED: 'badge badge-danger',
};

export default function SaleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const saleId = Number(id);
  const navigate = useNavigate();

  const saleQuery = useSaleDetail(saleId);
  const { cancelMutation } = useSaleMutations();
  const { isOwner } = useAuth();

  if (saleQuery.isLoading) return <p>불러오는 중...</p>;
  if (saleQuery.isError || !saleQuery.data) {
    return <p className="error-message">매출 전표를 찾을 수 없습니다.</p>;
  }

  const sale = saleQuery.data;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{sale.saleNo}</h1>
        <button type="button" onClick={() => navigate('/sales')}>
          목록으로
        </button>
      </div>
      <p>매출일자: {sale.saleDate}</p>
      <p>
        상태: <span className={STATUS_BADGE[sale.status]}>{STATUS_LABEL[sale.status]}</span>
      </p>
      <p>합계금액: {sale.totalAmount.toLocaleString()}</p>
      <p>메모: {sale.memo ?? '-'}</p>

      <h2>매출 품목</h2>
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
          {sale.items.map((item) => (
            <tr key={item.id}>
              <td>{item.itemSpecId}</td>
              <td>{item.quantity}</td>
              <td>{item.unitPrice.toLocaleString()}</td>
              <td>{item.amount.toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* 전표 취소는 재고와 원장을 되돌리므로 OWNER 전용(백엔드도 403으로 막는다) */}
      {sale.status === 'CONFIRMED' && isOwner && (
        <button
          type="button"
          className="button-danger"
          onClick={() => cancelMutation.mutate(sale.id)}
          disabled={cancelMutation.isPending}
        >
          {cancelMutation.isPending ? '취소 처리 중...' : '매출 전표 취소'}
        </button>
      )}
      {cancelMutation.isError && (
        <p className="error-message">{errorMessageOf(cancelMutation.error, '취소에 실패했습니다.')}</p>
      )}
    </div>
  );
}
