import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCompanyInfoList, useCompanyInfoMutations } from '../hooks/useCompanyInfo';
import type { CompanyInfoRequest } from '../types/api';

const emptyForm: CompanyInfoRequest = {
  companyName: '',
  businessNumber: '',
  ceoName: '',
  address: '',
  phone: '',
};

export default function CompanyInfoPage() {
  const listQuery = useCompanyInfoList();
  const { createMutation } = useCompanyInfoMutations();
  const [form, setForm] = useState<CompanyInfoRequest>(emptyForm);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate(form, { onSuccess: () => setForm(emptyForm) });
  }

  return (
    <div className="page">
      <h1>회사 정보</h1>

      {listQuery.isLoading && <p>불러오는 중...</p>}
      {listQuery.data && listQuery.data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>회사명</th>
              <th>사업자번호</th>
              <th>대표자</th>
              <th>주소</th>
              <th>전화번호</th>
            </tr>
          </thead>
          <tbody>
            {listQuery.data.map((info) => (
              <tr key={info.id}>
                <td>{info.companyName}</td>
                <td>{info.businessNumber ?? '-'}</td>
                <td>{info.ceoName ?? '-'}</td>
                <td>{info.address ?? '-'}</td>
                <td>{info.phone ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {listQuery.data && listQuery.data.length === 0 && (
        <p>등록된 회사 정보가 없습니다. 매입/매출 전표를 발행하려면 먼저 등록하세요.</p>
      )}

      <h2>회사 정보 등록</h2>
      <form onSubmit={handleSubmit}>
        <label>
          회사명
          <input
            type="text"
            value={form.companyName}
            onChange={(e) => setForm({ ...form, companyName: e.target.value })}
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
          대표자명
          <input
            type="text"
            value={form.ceoName}
            onChange={(e) => setForm({ ...form, ceoName: e.target.value })}
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
        <label>
          전화번호
          <input
            type="text"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
          />
        </label>
        <button type="submit" disabled={createMutation.isPending}>
          {createMutation.isPending ? '저장 중...' : '등록'}
        </button>
      </form>
    </div>
  );
}
