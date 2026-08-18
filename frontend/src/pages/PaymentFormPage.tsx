import { useState } from 'react';
import type { FormEvent } from 'react';
import type { AxiosError } from 'axios';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { usePartnerDetail } from '../hooks/usePartners';
import { usePaymentMutations } from '../hooks/usePayments';
import type { ErrorResponse, PaymentType } from '../types/api';

const TODAY = new Date().toISOString().slice(0, 10);

export default function PaymentFormPage() {
  const { id } = useParams<{ id: string }>();
  const partnerId = Number(id);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialType = searchParams.get('type') === 'DISBURSEMENT' ? 'DISBURSEMENT' : 'RECEIPT';

  const partnerQuery = usePartnerDetail(partnerId);
  const { createMutation } = usePaymentMutations();

  const [paymentType, setPaymentType] = useState<PaymentType>(initialType);
  const [amount, setAmount] = useState(0);
  const [paymentDate, setPaymentDate] = useState(TODAY);
  const [method, setMethod] = useState('');
  const [memo, setMemo] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate(
      { partnerId, paymentType, amount, paymentDate, method: method || undefined, memo: memo || undefined },
      { onSuccess: () => navigate(`/partners/${partnerId}`) },
    );
  }

  const errorMessage = (createMutation.error as AxiosError<ErrorResponse> | undefined)?.response?.data?.message;

  return (
    <div className="page">
      <h1>{paymentType === 'RECEIPT' ? '수금' : '지급'} 등록</h1>
      <p>거래처: {partnerQuery.data?.name ?? '...'}</p>

      <form onSubmit={handleSubmit}>
        <label>
          구분
          <select value={paymentType} onChange={(e) => setPaymentType(e.target.value as PaymentType)} required>
            <option value="RECEIPT">수금 (미수금 차감)</option>
            <option value="DISBURSEMENT">지급 (미지급금 차감)</option>
          </select>
        </label>
        <label>
          금액
          <input
            type="number"
            min={0.01}
            step={0.01}
            value={amount}
            onChange={(e) => setAmount(Number(e.target.value))}
            required
          />
        </label>
        <label>
          결제일자
          <input type="date" value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} required />
        </label>
        <label>
          결제수단
          <input type="text" value={method} onChange={(e) => setMethod(e.target.value)} maxLength={20} />
        </label>
        <label>
          메모
          <input type="text" value={memo} onChange={(e) => setMemo(e.target.value)} maxLength={255} />
        </label>

        {errorMessage && <p className="error-message">{errorMessage}</p>}

        <div className="form-actions">
          <button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? '등록 중...' : '등록'}
          </button>
          <button type="button" onClick={() => navigate(`/partners/${partnerId}`)}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
