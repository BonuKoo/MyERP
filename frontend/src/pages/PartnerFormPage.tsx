import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { fetchPartner } from '../api/partner';
import { usePartnerMutations } from '../hooks/usePartners';
import type { PartnerRequest, PartnerType } from '../types/api';

const emptyForm: PartnerRequest = {
  name: '',
  businessNumber: '',
  partnerType: 'CUSTOMER',
  contactName: '',
  contactPhone: '',
  address: '',
};

export default function PartnerFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = id !== undefined;
  const navigate = useNavigate();
  const { createMutation, updateMutation } = usePartnerMutations();

  const [form, setForm] = useState<PartnerRequest>(emptyForm);

  const partnerQuery = useQuery({
    queryKey: ['partner', id],
    queryFn: () => fetchPartner(Number(id)),
    enabled: isEdit,
  });

  useEffect(() => {
    if (partnerQuery.data) {
      const p = partnerQuery.data;
      setForm({
        name: p.name,
        businessNumber: p.businessNumber ?? '',
        partnerType: p.partnerType,
        contactName: p.contactName ?? '',
        contactPhone: p.contactPhone ?? '',
        address: p.address ?? '',
      });
    }
  }, [partnerQuery.data]);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (isEdit) {
      updateMutation.mutate(
        { id: Number(id), request: form },
        { onSuccess: () => navigate('/partners') },
      );
    } else {
      createMutation.mutate(form, { onSuccess: () => navigate('/partners') });
    }
  }

  const pending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="page">
      <h1>{isEdit ? '거래처 수정' : '새 거래처 등록'}</h1>
      <form onSubmit={handleSubmit}>
        <label>
          거래처명
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </label>
        <label>
          사업자번호
          <input
            type="text"
            value={form.businessNumber}
            onChange={(e) => setForm({ ...form, businessNumber: e.target.value })}
          />
        </label>
        <label>
          구분
          <select
            value={form.partnerType}
            onChange={(e) => setForm({ ...form, partnerType: e.target.value as PartnerType })}
          >
            <option value="CUSTOMER">매출처 (CUSTOMER)</option>
            <option value="SUPPLIER">매입처 (SUPPLIER)</option>
            <option value="BOTH">매입+매출 (BOTH)</option>
          </select>
        </label>
        <label>
          담당자명
          <input
            type="text"
            value={form.contactName}
            onChange={(e) => setForm({ ...form, contactName: e.target.value })}
          />
        </label>
        <label>
          담당자 연락처
          <input
            type="text"
            value={form.contactPhone}
            onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
          />
        </label>
        <label>
          주소
          <input
            type="text"
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={pending}>
            {pending ? '저장 중...' : '저장'}
          </button>
          <button type="button" onClick={() => navigate('/partners')}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
