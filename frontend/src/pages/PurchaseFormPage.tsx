import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePartnerList } from '../hooks/usePartners';
import { useCompanyInfoList } from '../hooks/useCompanyInfo';
import { useItemList, useItemSpecs } from '../hooks/useItems';
import { usePurchaseMutations } from '../hooks/usePurchases';
import type { PurchaseItemRequest } from '../types/api';

interface PurchaseLineState {
  key: number;
  itemId: number | null;
  itemSpecId: number | null;
  quantity: number;
  unitPrice: number;
}

function emptyLine(key: number): PurchaseLineState {
  return { key, itemId: null, itemSpecId: null, quantity: 1, unitPrice: 0 };
}

function PurchaseItemRow({
  line,
  onChange,
  onRemove,
}: {
  line: PurchaseLineState;
  onChange: (next: PurchaseLineState) => void;
  onRemove: () => void;
}) {
  const itemsQuery = useItemList(0, 100);
  const specsQuery = useItemSpecs(line.itemId);

  return (
    <tr>
      <td>
        <select
          value={line.itemId ?? ''}
          onChange={(e) => {
            const itemId = e.target.value ? Number(e.target.value) : null;
            onChange({ ...line, itemId, itemSpecId: null });
          }}
        >
          <option value="">품목 선택</option>
          {itemsQuery.data?.content.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </td>
      <td>
        <select
          value={line.itemSpecId ?? ''}
          onChange={(e) => {
            const itemSpecId = e.target.value ? Number(e.target.value) : null;
            const spec = specsQuery.data?.find((s) => s.id === itemSpecId);
            onChange({ ...line, itemSpecId, unitPrice: spec ? spec.costPrice : line.unitPrice });
          }}
          disabled={line.itemId === null}
        >
          <option value="">규격 선택</option>
          {specsQuery.data?.map((spec) => (
            <option key={spec.id} value={spec.id}>
              {spec.specName} ({spec.unit})
            </option>
          ))}
        </select>
      </td>
      <td>
        <input
          type="number"
          min={1}
          value={line.quantity}
          onChange={(e) => onChange({ ...line, quantity: Number(e.target.value) })}
        />
      </td>
      <td>
        <input
          type="number"
          min={0}
          value={line.unitPrice}
          onChange={(e) => onChange({ ...line, unitPrice: Number(e.target.value) })}
        />
      </td>
      <td>{(line.quantity * line.unitPrice).toLocaleString()}</td>
      <td>
        <button type="button" onClick={onRemove}>
          삭제
        </button>
      </td>
    </tr>
  );
}

export default function PurchaseFormPage() {
  const navigate = useNavigate();
  const partnersQuery = usePartnerList(0, 100);
  const companyInfoQuery = useCompanyInfoList();
  const { createMutation } = usePurchaseMutations();
  const lineKeySeq = useRef(0);

  const [partnerId, setPartnerId] = useState<number | null>(null);
  const [companyInfoId, setCompanyInfoId] = useState<number | null>(null);
  const [purchaseDate, setPurchaseDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [memo, setMemo] = useState('');
  const [lines, setLines] = useState<PurchaseLineState[]>([emptyLine(lineKeySeq.current++)]);

  function addLine() {
    setLines((prev) => [...prev, emptyLine(lineKeySeq.current++)]);
  }

  function updateLine(index: number, next: PurchaseLineState) {
    setLines((prev) => prev.map((line, i) => (i === index ? next : line)));
  }

  function removeLine(index: number) {
    setLines((prev) => prev.filter((_, i) => i !== index));
  }

  const totalAmount = lines.reduce((sum, line) => sum + line.quantity * line.unitPrice, 0);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (partnerId === null || companyInfoId === null) return;

    const items: PurchaseItemRequest[] = lines
      .filter((line) => line.itemSpecId !== null)
      .map((line) => ({
        itemSpecId: line.itemSpecId as number,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
      }));

    if (items.length === 0) return;

    createMutation.mutate(
      { partnerId, companyInfoId, purchaseDate, memo: memo || undefined, items },
      { onSuccess: (created) => navigate(`/purchases/${created.id}`) },
    );
  }

  return (
    <div className="page">
      <h1>새 매입 전표 등록</h1>
      <form className="form-wide" onSubmit={handleSubmit}>
        <label>
          거래처
          <select
            value={partnerId ?? ''}
            onChange={(e) => setPartnerId(e.target.value ? Number(e.target.value) : null)}
            required
          >
            <option value="">선택하세요</option>
            {partnersQuery.data?.content.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          회사 정보
          <select
            value={companyInfoId ?? ''}
            onChange={(e) => setCompanyInfoId(e.target.value ? Number(e.target.value) : null)}
            required
          >
            <option value="">선택하세요</option>
            {companyInfoQuery.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.companyName}
              </option>
            ))}
          </select>
        </label>
        <label>
          매입일자
          <input
            type="date"
            value={purchaseDate}
            onChange={(e) => setPurchaseDate(e.target.value)}
            required
          />
        </label>
        <label>
          메모
          <input type="text" value={memo} onChange={(e) => setMemo(e.target.value)} />
        </label>

        <h3>매입 품목</h3>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>품목</th>
                <th>규격</th>
                <th>수량</th>
                <th>단가</th>
                <th>금액</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {lines.map((line, index) => (
                <PurchaseItemRow
                  key={line.key}
                  line={line}
                  onChange={(next) => updateLine(index, next)}
                  onRemove={() => removeLine(index)}
                />
              ))}
            </tbody>
          </table>
        </div>
        <button type="button" onClick={addLine}>
          품목 추가
        </button>

        <p>합계: {totalAmount.toLocaleString()}</p>

        <div className="form-actions">
          <button type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? '저장 중...' : '저장'}
          </button>
          <button type="button" onClick={() => navigate('/purchases')}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
