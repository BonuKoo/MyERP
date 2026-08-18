import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePartnerDetail } from '../hooks/usePartners';
import { usePartnerLedger } from '../hooks/usePartnerLedger';
import { usePaymentMutations } from '../hooks/usePayments';
import { useAuth } from '../auth/AuthContext';
import type { LedgerChangeType, LedgerEntryResponse } from '../types/api';

const PAGE_SIZE = 20;

const CHANGE_TYPE_LABEL: Record<LedgerChangeType, string> = {
  SALE_CONFIRMED: '매출 확정',
  SALE_CANCELED: '매출 취소',
  PURCHASE_CONFIRMED: '매입 확정',
  PURCHASE_CANCELED: '매입 취소',
  PAYMENT_RECEIVED: '수금',
  PAYMENT_RECEIVED_CANCELED: '수금 취소',
  PAYMENT_PAID: '지급',
  PAYMENT_PAID_CANCELED: '지급 취소',
};

/**
 * 결제(수금/지급)만 취소할 수 있다 — 매출/매입 이력은 각자의 화면(매출/매입 전표)에서
 * 취소한다. relatedDocumentId가 곧 결제 전표 id다(LedgerService가 그렇게 기록함).
 */
function isCancelablePayment(changeType: LedgerChangeType): boolean {
  return changeType === 'PAYMENT_RECEIVED' || changeType === 'PAYMENT_PAID';
}

/**
 * 원장 엔트리 자체에는 "이 결제가 이미 취소됐는지"가 없다(changeType은 등록 당시
 * 값 그대로 남는다). 취소하면 별도의 *_CANCELED 엔트리가 새로 쌓이므로, 현재 로드된
 * 페이지 안에서 그 짝을 찾아 원본 쪽 취소 버튼을 숨긴다.
 *
 * 원본과 취소 엔트리가 페이지 경계에 걸리면(20건보다 오래전에 취소된 건) 놓칠 수
 * 있다는 한계가 있다 — 그 경우 버튼을 눌러도 백엔드가 409로 막아 안전하지만,
 * 죽은 버튼이 잠깐 보일 수 있다.
 */
function findAlreadyCanceledPaymentIds(entries: LedgerEntryResponse[]): Set<number> {
  const canceled = new Set<number>();
  for (const entry of entries) {
    if (
      (entry.changeType === 'PAYMENT_RECEIVED_CANCELED' || entry.changeType === 'PAYMENT_PAID_CANCELED') &&
      entry.relatedDocumentId
    ) {
      canceled.add(entry.relatedDocumentId);
    }
  }
  return canceled;
}

export default function PartnerDetailPage() {
  const { id } = useParams<{ id: string }>();
  const partnerId = Number(id);
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const partnerQuery = usePartnerDetail(partnerId);
  const ledgerQuery = usePartnerLedger(partnerId, page, PAGE_SIZE);
  const { cancelMutation } = usePaymentMutations();
  const { isOwner } = useAuth();

  // 조기 return보다 먼저 호출해야 훅 호출 순서가 렌더마다 일정하게 유지된다.
  const ledger = ledgerQuery.data;
  const alreadyCanceledPaymentIds = useMemo(
    () => findAlreadyCanceledPaymentIds(ledger?.content ?? []),
    [ledger],
  );

  if (partnerQuery.isLoading) return <p>불러오는 중...</p>;
  if (partnerQuery.isError || !partnerQuery.data) {
    return <p className="error-message">거래처를 찾을 수 없습니다.</p>;
  }

  const partner = partnerQuery.data;
  const totalPages = ledger ? Math.max(1, Math.ceil(ledger.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{partner.name}</h1>
        <button type="button" onClick={() => navigate('/partners')}>
          목록으로
        </button>
      </div>

      <p>구분: {partner.partnerType}</p>
      <p>담당자: {partner.contactName ?? '-'}</p>
      <p>연락처: {partner.contactPhone ?? '-'}</p>

      <h2>잔액</h2>
      <table>
        <thead>
          <tr>
            <th>미수금</th>
            <th>미지급금</th>
            <th />
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{partner.receivableBalance.toLocaleString()}</td>
            <td>{partner.payableBalance.toLocaleString()}</td>
            <td>
              {/* 수금/지급은 돈이 오가므로 OWNER 전용(백엔드도 403으로 막는다) */}
              {isOwner ? (
                <>
                  <Link to={`/partners/${partnerId}/payments/new?type=RECEIPT`}>
                    <button type="button">수금 등록</button>
                  </Link>
                  <Link to={`/partners/${partnerId}/payments/new?type=DISBURSEMENT`}>
                    <button type="button">지급 등록</button>
                  </Link>
                </>
              ) : (
                <span className="form-hint">수금/지급 등록은 사업주만 가능합니다.</span>
              )}
            </td>
          </tr>
        </tbody>
      </table>

      <h2>원장 이력</h2>
      {ledgerQuery.isLoading && <p>불러오는 중...</p>}
      {ledgerQuery.isError && <p className="error-message">원장 이력을 불러오지 못했습니다.</p>}
      {ledger && (
        <>
          <table>
            <thead>
              <tr>
                <th>일시</th>
                <th>구분</th>
                <th>내용</th>
                <th>금액</th>
                <th>잔액</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {ledger.content.map((entry) => (
                <tr key={entry.id}>
                  <td>{entry.createdAt.slice(0, 16).replace('T', ' ')}</td>
                  <td>{entry.ledgerType === 'RECEIVABLE' ? '미수금' : '미지급금'}</td>
                  <td>{CHANGE_TYPE_LABEL[entry.changeType]}</td>
                  <td>{entry.amount.toLocaleString()}</td>
                  <td>{entry.balanceAfter.toLocaleString()}</td>
                  <td>
                    {isOwner &&
                      isCancelablePayment(entry.changeType) &&
                      entry.relatedDocumentId &&
                      !alreadyCanceledPaymentIds.has(entry.relatedDocumentId) && (
                        <button
                          type="button"
                          onClick={() => cancelMutation.mutate(entry.relatedDocumentId as number)}
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
        </>
      )}
      {cancelMutation.isError && <p className="error-message">결제 취소에 실패했습니다.</p>}
    </div>
  );
}
