import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCategoryMains, useCategoryMutations, useCategorySubs } from '../hooks/useCategories';
import { useAuth } from '../auth/AuthContext';

export default function CategoryPage() {
  const [selectedMainId, setSelectedMainId] = useState<number | null>(null);
  const [newMainName, setNewMainName] = useState('');
  const [newSubName, setNewSubName] = useState('');
  const { isOwner } = useAuth();

  const mainsQuery = useCategoryMains();
  const subsQuery = useCategorySubs(selectedMainId);
  const { createMainMutation, createSubMutation } = useCategoryMutations();

  function handleCreateMain(e: FormEvent) {
    e.preventDefault();
    if (!newMainName.trim()) return;
    createMainMutation.mutate(
      { name: newMainName, displayOrder: (mainsQuery.data?.length ?? 0) + 1 },
      { onSuccess: () => setNewMainName('') },
    );
  }

  function handleCreateSub(e: FormEvent) {
    e.preventDefault();
    if (!newSubName.trim() || selectedMainId === null) return;
    createSubMutation.mutate(
      { categoryMainId: selectedMainId, name: newSubName, displayOrder: (subsQuery.data?.length ?? 0) + 1 },
      { onSuccess: () => setNewSubName('') },
    );
  }

  return (
    <div className="page">
      <h1>카테고리 관리</h1>
      <div className="category-columns">
        <section>
          <h2>대분류</h2>
          <ul className="category-list">
            {mainsQuery.data?.map((main) => (
              <li key={main.id}>
                <button
                  type="button"
                  className={main.id === selectedMainId ? 'selected' : ''}
                  onClick={() => setSelectedMainId(main.id)}
                >
                  {main.name}
                </button>
              </li>
            ))}
          </ul>
          {/* 분류 체계는 마스터 데이터라 등록은 OWNER 전용(백엔드도 403으로 막는다) */}
          {isOwner && (
            <form onSubmit={handleCreateMain} className="inline-form">
              <input
                type="text"
                placeholder="새 대분류명"
                value={newMainName}
                onChange={(e) => setNewMainName(e.target.value)}
              />
              <button type="submit" disabled={createMainMutation.isPending}>
                추가
              </button>
            </form>
          )}
        </section>

        <section>
          <h2>중분류</h2>
          {selectedMainId === null ? (
            <p>대분류를 선택하세요.</p>
          ) : (
            <>
              <ul className="category-list">
                {subsQuery.data?.map((sub) => (
                  <li key={sub.id}>{sub.name}</li>
                ))}
              </ul>
              {isOwner && (
                <form onSubmit={handleCreateSub} className="inline-form">
                  <input
                    type="text"
                    placeholder="새 중분류명"
                    value={newSubName}
                    onChange={(e) => setNewSubName(e.target.value)}
                  />
                  <button type="submit" disabled={createSubMutation.isPending}>
                    추가
                  </button>
                </form>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
