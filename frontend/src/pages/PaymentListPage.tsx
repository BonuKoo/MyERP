import { useState } from 'react';
import { Link } from 'react-router-dom';
import { usePaymentList, usePaymentMutations } from '../hooks/usePayments';
import { useAuth } from '../auth/AuthContext';
import { errorMessageOf } from '../api/client';

const PAGE_SIZE = 20;

export default function PaymentListPage() {
  const [page, setPage] = useState(0);
  const paymentsQuery = usePaymentList(page, PAGE_SIZE);
  const { cancelMutation } = usePaymentMutations();
  const { isOwner } = useAuth();

  if (paymentsQuery.isLoading) return <p>불러오는 중...</p>;
  if (paymentsQuery.isError) {
    return <p className="error-message">결제 목록을 불러오지 못했습니다.</p>;
  }

  const data = paymentsQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>결제 내역</h1>
      </div>

      <table>
        <thead>
          <tr>
            <th>전표번호</th>
            <th>거래처</th>
            <th>구분</th>
            <th>금액</th>
            <th>결제일자</th>
            <th>결제수단</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {data?.content.map((payment) => (
            <tr key={payment.id}>
              <td>{payment.paymentNo}</td>
              <td>
                <Link to={`/partners/${payment.partnerId}`}>#{payment.partnerId}</Link>
              </td>
              <td>{payment.paymentType === 'RECEIPT' ? '수금' : '지급'}</td>
              <td>{payment.amount.toLocaleString()}</td>
              <td>{payment.paymentDate}</td>
              <td>{payment.method ?? '-'}</td>
              <td>
                <span className={payment.status === 'CONFIRMED' ? 'badge badge-success' : 'badge badge-danger'}>
                  {payment.status === 'CONFIRMED' ? '확정' : '취소됨'}
                </span>
              </td>
              <td>
                {/* 결제 취소는 OWNER 전용(백엔드도 403으로 막는다) */}
                {payment.status === 'CONFIRMED' && isOwner && (
                  <button
                    type="button"
                    className="button-danger"
                    onClick={() => cancelMutation.mutate(payment.id)}
                    disabled={cancelMutation.isPending}
                  >
                    취소
                  </button>
                )}
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
        <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
          다음
        </button>
      </div>
      {cancelMutation.isError && (
        <p className="error-message">{errorMessageOf(cancelMutation.error, '결제 취소에 실패했습니다.')}</p>
      )}
    </div>
  );
}
