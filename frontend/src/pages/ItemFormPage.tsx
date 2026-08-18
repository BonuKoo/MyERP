import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCategoryMains, useCategorySubs } from '../hooks/useCategories';
import { useCertificationList } from '../hooks/useCertifications';
import { useItemMutations } from '../hooks/useItems';

export default function ItemFormPage() {
  const navigate = useNavigate();
  const [mainId, setMainId] = useState<number | null>(null);
  const [subId, setSubId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [ksStandard, setKsStandard] = useState('');
  const [selectedCertIds, setSelectedCertIds] = useState<number[]>([]);

  const mainsQuery = useCategoryMains();
  const subsQuery = useCategorySubs(mainId);
  const certsQuery = useCertificationList();
  const { createMutation } = useItemMutations();

  function toggleCert(id: number) {
    setSelectedCertIds((prev) => (prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]));
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (subId === null) return;
    createMutation.mutate(
      {
        categorySubId: subId,
        name,
        description: description || undefined,
        ksStandard: ksStandard || undefined,
        certificationIds: selectedCertIds,
      },
      { onSuccess: (created) => navigate(`/items/${created.id}`) },
    );
  }

  return (
    <div className="page">
      <h1>새 품목 등록</h1>
      <form onSubmit={handleSubmit}>
        <label>
          대분류
          <select
            value={mainId ?? ''}
            onChange={(e) => {
              const value = e.target.value ? Number(e.target.value) : null;
              setMainId(value);
              setSubId(null);
            }}
            required
          >
            <option value="">선택하세요</option>
            {mainsQuery.data?.map((main) => (
              <option key={main.id} value={main.id}>
                {main.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          중분류
          <select
            value={subId ?? ''}
            onChange={(e) => setSubId(e.target.value ? Number(e.target.value) : null)}
            disabled={mainId === null}
            required
          >
            <option value="">선택하세요</option>
            {subsQuery.data?.map((sub) => (
              <option key={sub.id} value={sub.id}>
                {sub.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          품목명
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          설명
          <input type="text" value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        <label>
          KS규격
          <input type="text" value={ksStandard} onChange={(e) => setKsStandard(e.target.value)} />
        </label>
        <fieldset>
          <legend>인증정보</legend>
          {certsQuery.data?.map((cert) => (
            <label key={cert.id} className="checkbox-label">
              <input
                type="checkbox"
                checked={selectedCertIds.includes(cert.id)}
                onChange={() => toggleCert(cert.id)}
              />
              {cert.name}
            </label>
          ))}
          {(certsQuery.data?.length ?? 0) === 0 && <p>등록된 인증정보가 없습니다.</p>}
        </fieldset>
        <div className="form-actions">
          <button type="submit" disabled={createMutation.isPending || subId === null}>
            {createMutation.isPending ? '저장 중...' : '저장'}
          </button>
          <button type="button" onClick={() => navigate('/items')}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
