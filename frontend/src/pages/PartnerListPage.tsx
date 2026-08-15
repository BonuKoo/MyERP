import { useState } from 'react';
import { Link } from 'react-router-dom';
import { usePartnerList, usePartnerMutations } from '../hooks/usePartners';

const PAGE_SIZE = 20;

export default function PartnerListPage() {
  const [page, setPage] = useState(0);
  const partnersQuery = usePartnerList(page, PAGE_SIZE);
  const { deactivateMutation } = usePartnerMutations();

  if (partnersQuery.isLoading) return <p>불러오는 중...</p>;
  if (partnersQuery.isError) {
    return <p className="error-message">거래처 목록을 불러오지 못했습니다.</p>;
  }

  const data = partnersQuery.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.totalCount / PAGE_SIZE)) : 1;

  return (
    <div className="page">
      <div className="page-header">
        <h1>거래처 관리</h1>
        <Link to="/partners/new">
          <button type="button">새 거래처 등록</button>
        </Link>
      </div>

      <table>
        <thead>
          <tr>
            <th>이름</th>
            <th>구분</th>
            <th>담당자</th>
            <th>연락처</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {data?.content.map((partner) => (
            <tr key={partner.id}>
              <td>{partner.name}</td>
              <td>{partner.partnerType}</td>
              <td>{partner.contactName ?? '-'}</td>
              <td>{partner.contactPhone ?? '-'}</td>
              <td>{partner.active ? '활성' : '비활성'}</td>
              <td>
                <Link to={`/partners/${partner.id}/edit`}>수정</Link>
                {partner.active && (
                  <button
                    type="button"
                    onClick={() => deactivateMutation.mutate(partner.id)}
                    disabled={deactivateMutation.isPending}
                  >
                    비활성화
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
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          다음
        </button>
      </div>
    </div>
  );
}
