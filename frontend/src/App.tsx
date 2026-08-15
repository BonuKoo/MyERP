import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import PartnerListPage from './pages/PartnerListPage';
import PartnerFormPage from './pages/PartnerFormPage';
import CategoryPage from './pages/CategoryPage';
import ItemListPage from './pages/ItemListPage';
import ItemFormPage from './pages/ItemFormPage';
import ItemDetailPage from './pages/ItemDetailPage';

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
                <Route path="/partners/:id/edit" element={<PartnerFormPage />} />
                <Route path="/categories" element={<CategoryPage />} />
                <Route path="/items" element={<ItemListPage />} />
                <Route path="/items/new" element={<ItemFormPage />} />
                <Route path="/items/:id" element={<ItemDetailPage />} />
              </Route>

              <Route path="*" element={<Navigate to="/partners" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
