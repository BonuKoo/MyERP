import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useItemDetail, useItemSpecMutations, useItemSpecs } from '../hooks/useItems';
import { useStockAdjustMutation, useStockHistory } from '../hooks/useStock';
import type { ItemSpecRequest } from '../types/api';

const emptySpecForm: ItemSpecRequest = {
  specName: '',
  unit: '',
  costPrice: 0,
  salePrice: 0,
  safetyStock: 0,
};

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>();
  const itemId = Number(id);
  const navigate = useNavigate();

  const itemQuery = useItemDetail(itemId);
  const specsQuery = useItemSpecs(itemId);
  const { createSpecMutation } = useItemSpecMutations(itemId);
  const stockAdjustMutation = useStockAdjustMutation(itemId);

  const [specForm, setSpecForm] = useState<ItemSpecRequest>(emptySpecForm);
  const [adjustDeltas, setAdjustDeltas] = useState<Record<number, string>>({});
  const [historySpecId, setHistorySpecId] = useState<number | null>(null);

  const historyQuery = useStockHistory(historySpecId, 0, 10);

  function handleCreateSpec(e: FormEvent) {
    e.preventDefault();
    createSpecMutation.mutate(specForm, { onSuccess: () => setSpecForm(emptySpecForm) });
  }

  function handleAdjust(specId: number) {
    const raw = adjustDeltas[specId];
    const delta = Number(raw);
    if (!raw || Number.isNaN(delta) || delta === 0) return;
    stockAdjustMutation.mutate(
      { itemSpecId: specId, request: { quantityDelta: delta } },
      { onSuccess: () => setAdjustDeltas((prev) => ({ ...prev, [specId]: '' })) },
    );
  }

  if (itemQuery.isLoading) return <p>불러오는 중...</p>;
  if (itemQuery.isError || !itemQuery.data) {
    return <p className="error-message">품목을 찾을 수 없습니다.</p>;
  }

  const item = itemQuery.data;

  return (
    <div className="page">
      <div className="page-header">
        <h1>{item.name}</h1>
        <button type="button" onClick={() => navigate('/items')}>
          목록으로
        </button>
      </div>
      <p>KS규격: {item.ksStandard ?? '-'}</p>
      <p>인증정보: {item.certifications.map((c) => c.name).join(', ') || '-'}</p>

      <h2>규격 목록</h2>
      <table>
        <thead>
          <tr>
            <th>규격</th>
            <th>단위</th>
            <th>매입가</th>
            <th>매출가</th>
            <th>현재재고</th>
            <th>안전재고</th>
            <th>재고조정</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {specsQuery.data?.map((spec) => (
            <tr key={spec.id}>
              <td>{spec.specName}</td>
              <td>{spec.unit}</td>
              <td>{spec.costPrice.toLocaleString()}</td>
              <td>{spec.salePrice.toLocaleString()}</td>
              <td>{spec.currentStock}</td>
              <td>{spec.safetyStock}</td>
              <td>
                <input
                  type="number"
                  className="stock-adjust-input"
                  value={adjustDeltas[spec.id] ?? ''}
                  onChange={(e) => setAdjustDeltas((prev) => ({ ...prev, [spec.id]: e.target.value }))}
                  placeholder="+10 / -5"
                />
                <button
                  type="button"
                  onClick={() => handleAdjust(spec.id)}
                  disabled={stockAdjustMutation.isPending}
                >
                  적용
                </button>
              </td>
              <td>
                <button type="button" onClick={() => setHistorySpecId(spec.id)}>
                  이력보기
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {stockAdjustMutation.isError && (
        <p className="error-message">재고 조정에 실패했습니다. (재고가 부족할 수 있습니다)</p>
      )}

      <h3>새 규격 등록</h3>
      <form onSubmit={handleCreateSpec} className="inline-form-block">
        <label>
          규격명
          <input
            type="text"
            value={specForm.specName}
            onChange={(e) => setSpecForm({ ...specForm, specName: e.target.value })}
            required
          />
        </label>
        <label>
          단위
          <input
            type="text"
            value={specForm.unit}
            onChange={(e) => setSpecForm({ ...specForm, unit: e.target.value })}
            required
          />
        </label>
        <label>
          매입가
          <input
            type="number"
            value={specForm.costPrice}
            onChange={(e) => setSpecForm({ ...specForm, costPrice: Number(e.target.value) })}
            required
          />
        </label>
        <label>
          매출가
          <input
            type="number"
            value={specForm.salePrice}
            onChange={(e) => setSpecForm({ ...specForm, salePrice: Number(e.target.value) })}
            required
          />
        </label>
        <label>
          안전재고
          <input
            type="number"
            value={specForm.safetyStock}
            onChange={(e) => setSpecForm({ ...specForm, safetyStock: Number(e.target.value) })}
          />
        </label>
        <button type="submit" disabled={createSpecMutation.isPending}>
          규격 추가
        </button>
      </form>

      {historySpecId !== null && (
        <div className="history-panel">
          <h3>재고 이력 (규격 #{historySpecId})</h3>
          <button type="button" onClick={() => setHistorySpecId(null)}>
            닫기
          </button>
          <table>
            <thead>
              <tr>
                <th>구분</th>
                <th>변동수량</th>
                <th>변동전</th>
                <th>변동후</th>
                <th>일시</th>
              </tr>
            </thead>
            <tbody>
              {historyQuery.data?.content.map((h) => (
                <tr key={h.id}>
                  <td>{h.changeType}</td>
                  <td>{h.quantity}</td>
                  <td>{h.beforeStock}</td>
                  <td>{h.afterStock}</td>
                  <td>{new Date(h.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
