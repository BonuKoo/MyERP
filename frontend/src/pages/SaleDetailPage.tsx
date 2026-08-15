import { useNavigate, useParams } from 'react-router-dom';
import type { AxiosError } from 'axios';
import { useSaleDetail, useSaleMutations } from '../hooks/useSales';
import type { ErrorResponse } from '../types/api';

export default function SaleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const saleId = Number(id);
  const navigate = useNavigate();

  const saleQuery = useSaleDetail(saleId);
  const { cancelMutation } = useSaleMutations();

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
      <p>상태: {sale.status}</p>
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

      {sale.status === 'CONFIRMED' && (
        <button
          type="button"
          onClick={() => cancelMutation.mutate(sale.id)}
          disabled={cancelMutation.isPending}
        >
          {cancelMutation.isPending ? '취소 처리 중...' : '매출 전표 취소'}
        </button>
      )}
      {cancelMutation.isError && (
        <p className="error-message">
          {(cancelMutation.error as AxiosError<ErrorResponse>).response?.data?.message ??
            '취소에 실패했습니다.'}
        </p>
      )}
    </div>
  );
}
