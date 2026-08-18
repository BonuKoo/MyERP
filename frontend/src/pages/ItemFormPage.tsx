import { useEffect, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAllCategorySubs, useCategoryMains, useCategorySubs } from '../hooks/useCategories';
import { useCertificationList } from '../hooks/useCertifications';
import { useItemDetail, useItemImageMutations, useItemMutations } from '../hooks/useItems';
import { itemImageUrl, uploadItemImages } from '../api/item';

export default function ItemFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = id !== undefined;
  const itemId = isEdit ? Number(id) : null;
  const navigate = useNavigate();

  const [mainId, setMainId] = useState<number | null>(null);
  const [subId, setSubId] = useState<number | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [ksStandard, setKsStandard] = useState('');
  const [selectedCertIds, setSelectedCertIds] = useState<number[]>([]);
  // 아직 서버에 올리지 않은 파일들. 등록 화면에서는 품목이 만들어진 뒤에야 업로드할 수
  // 있으므로(사진 API가 itemId를 요구한다) 저장 버튼을 누를 때까지 여기 담아둔다.
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const mainsQuery = useCategoryMains();
  const subsQuery = useCategorySubs(mainId);
  const certsQuery = useCertificationList();
  const itemQuery = useItemDetail(itemId);
  const allSubsQuery = useAllCategorySubs();
  const { createMutation, updateMutation } = useItemMutations();
  const { uploadMutation, deleteMutation } = useItemImageMutations(itemId ?? 0);

  // 수정 모드에서는 기존 값을 폼에 채운다. 중분류만 저장돼 있으므로 그 중분류가 속한
  // 대분류를 역으로 찾아 연쇄 선택 상태를 복원해야 한다.
  useEffect(() => {
    const item = itemQuery.data;
    if (!item) return;
    setName(item.name);
    setDescription(item.description ?? '');
    setKsStandard(item.ksStandard ?? '');
    setSelectedCertIds(item.certifications.map((c) => c.id));
    setSubId(item.categorySubId);
  }, [itemQuery.data]);

  useEffect(() => {
    const item = itemQuery.data;
    if (!item || mainId !== null) return;
    const sub = allSubsQuery.data?.find((s) => s.id === item.categorySubId);
    if (sub) setMainId(sub.categoryMainId);
  }, [itemQuery.data, allSubsQuery.data, mainId]);

  function toggleCert(certId: number) {
    setSelectedCertIds((prev) =>
      prev.includes(certId) ? prev.filter((c) => c !== certId) : [...prev, certId],
    );
  }

  function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    setPendingFiles(Array.from(e.target.files ?? []));
    setUploadError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (subId === null) return;
    setUploadError(null);

    const request = {
      categorySubId: subId,
      name,
      description: description || undefined,
      ksStandard: ksStandard || undefined,
      certificationIds: selectedCertIds,
    };

    try {
      if (isEdit) {
        await updateMutation.mutateAsync({ id: itemId as number, request });
        if (pendingFiles.length > 0) {
          await uploadMutation.mutateAsync(pendingFiles);
        }
        navigate(`/items/${itemId}`);
      } else {
        const created = await createMutation.mutateAsync(request);
        // 품목을 먼저 만들고 사진을 올리는 2단계다(사진 API가 itemId를 요구하므로).
        // 등록 시점엔 훅에 넘길 itemId가 없어서 api 함수를 직접 호출한다.
        // 사진 업로드만 실패해도 품목은 이미 저장돼 있으므로, 상세 화면으로 보내고
        // 거기서 다시 붙이도록 안내한다.
        try {
          if (pendingFiles.length > 0) {
            await uploadItemImages(created.id, pendingFiles);
          }
        } catch {
          setUploadError('품목은 저장됐지만 사진 업로드에 실패했습니다. 수정 화면에서 다시 시도해주세요.');
        }
        navigate(`/items/${created.id}`);
      }
    } catch {
      setUploadError('저장에 실패했습니다.');
    }
  }

  const pending = createMutation.isPending || updateMutation.isPending || uploadMutation.isPending;
  const existingImages = itemQuery.data?.images ?? [];

  return (
    <div className="page">
      <h1>{isEdit ? '품목 수정' : '새 품목 등록'}</h1>
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

        <fieldset>
          <legend>사진</legend>

          {isEdit && existingImages.length > 0 && (
            <ul className="image-edit-list">
              {existingImages.map((image) => (
                <li key={image.id}>
                  <img src={itemImageUrl(itemId as number, image.id, 'thumb')} alt={image.uploadFileName} />
                  {image.primary && <span className="badge badge-success">대표</span>}
                  <button
                    type="button"
                    className="button-danger"
                    onClick={() => deleteMutation.mutate(image.id)}
                    disabled={deleteMutation.isPending}
                  >
                    삭제
                  </button>
                </li>
              ))}
            </ul>
          )}

          <input type="file" accept="image/png,image/jpeg,image/gif" multiple onChange={handleFileChange} />
          <span className="form-hint">jpg/png/gif, 파일당 5MB까지. 첫 사진이 목록 카드의 대표 사진이 됩니다.</span>

          {/* 올리기 전에 브라우저에서 바로 미리보기를 만든다(서버 왕복 없음). */}
          {pendingFiles.length > 0 && (
            <ul className="image-preview-list">
              {pendingFiles.map((file) => (
                <li key={file.name + file.lastModified}>
                  <img src={URL.createObjectURL(file)} alt={file.name} />
                  <span>{file.name}</span>
                </li>
              ))}
            </ul>
          )}
        </fieldset>

        {uploadError && <p className="error-message">{uploadError}</p>}

        <div className="form-actions">
          <button type="submit" disabled={pending || subId === null}>
            {pending ? '저장 중...' : '저장'}
          </button>
          <button type="button" onClick={() => navigate(isEdit ? `/items/${itemId}` : '/items')}>
            취소
          </button>
        </div>
      </form>
    </div>
  );
}
