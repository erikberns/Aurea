import { Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { CartProvider } from "./context/CartContext";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import AdminLayout from "./components/admin/AdminLayout";

import Landing from "./pages/Landing";
import Catalogo from "./pages/Catalogo";
import ProductoDetalle from "./pages/ProductoDetalle";
import Checkout from "./pages/Checkout";
import Login from "./pages/Login";
import Registro from "./pages/Registro";
import Cuenta from "./pages/Cuenta";
import AdminPanel from "./pages/admin/AdminPanel";
import AdminProductos from "./pages/admin/AdminProductos";
import AdminProductoForm from "./pages/admin/AdminProductoForm";
import AdminPedidos from "./pages/admin/AdminPedidos";
import AdminUsuarios from "./pages/admin/AdminUsuarios";
import AdminReposiciones from "./pages/admin/AdminReposiciones";
import AdminAvisos from "./pages/admin/AdminAvisos";
import { ThemeProvider } from "./context/ThemeContext";

export default function App() {
  return (
    <ThemeProvider>
    <AuthProvider>
      <CartProvider>
        <Routes>
          {/* Tienda pública: cabecera y pie de la tienda. */}
          <Route element={<Layout />}>
            <Route path="/" element={<Landing />} />
            <Route path="/catalogo" element={<Catalogo />} />
            <Route path="/productos/:id" element={<ProductoDetalle />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/login" element={<Login />} />
            <Route path="/registro" element={<Registro />} />
            <Route
              path="/cuenta"
              element={
                <ProtectedRoute>
                  <Cuenta />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Panel de administración: solo rol ADMIN (el backend vuelve a verificarlo en cada endpoint). */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute rolRequerido="ADMIN">
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<AdminPanel />} />
            <Route path="productos" element={<AdminProductos />} />
            <Route path="productos/nueva" element={<AdminProductoForm />} />
            <Route path="productos/:id" element={<AdminProductoForm />} />
            <Route path="pedidos" element={<AdminPedidos />} />
            <Route path="usuarios" element={<AdminUsuarios />} />
            <Route path="reposiciones" element={<AdminReposiciones />} />
            <Route path="avisos" element={<AdminAvisos />} />
            <Route path="*" element={<Navigate to="/admin" replace />} />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
    </ThemeProvider>
  );
}
