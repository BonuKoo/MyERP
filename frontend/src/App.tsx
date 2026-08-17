import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import PartnerListPage from './pages/PartnerListPage';
import PartnerFormPage from './pages/PartnerFormPage';
import PartnerDetailPage from './pages/PartnerDetailPage';
import PaymentFormPage from './pages/PaymentFormPage';
import CategoryPage from './pages/CategoryPage';
import ItemListPage from './pages/ItemListPage';
import ItemFormPage from './pages/ItemFormPage';
import ItemDetailPage from './pages/ItemDetailPage';
import CompanyInfoPage from './pages/CompanyInfoPage';
import PurchaseListPage from './pages/PurchaseListPage';
import PurchaseFormPage from './pages/PurchaseFormPage';
import PurchaseDetailPage from './pages/PurchaseDetailPage';
import SaleListPage from './pages/SaleListPage';
import SaleFormPage from './pages/SaleFormPage';
import SaleDetailPage from './pages/SaleDetailPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Navigate to="/partners" replace />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />

              <Route element={<ProtectedRoute />}>
                <Route path="/partners" element={<PartnerListPage />} />
                <Route path="/partners/new" element={<PartnerFormPage />} />
                <Route path="/partners/:id" element={<PartnerDetailPage />} />
                <Route path="/partners/:id/edit" element={<PartnerFormPage />} />
                <Route path="/partners/:id/payments/new" element={<PaymentFormPage />} />
                <Route path="/categories" element={<CategoryPage />} />
                <Route path="/items" element={<ItemListPage />} />
                <Route path="/items/new" element={<ItemFormPage />} />
                <Route path="/items/:id" element={<ItemDetailPage />} />
                <Route path="/company-info" element={<CompanyInfoPage />} />
                <Route path="/purchases" element={<PurchaseListPage />} />
                <Route path="/purchases/new" element={<PurchaseFormPage />} />
                <Route path="/purchases/:id" element={<PurchaseDetailPage />} />
                <Route path="/sales" element={<SaleListPage />} />
                <Route path="/sales/new" element={<SaleFormPage />} />
                <Route path="/sales/:id" element={<SaleDetailPage />} />
              </Route>

              <Route path="*" element={<Navigate to="/partners" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
