import { createBrowserRouter } from "react-router-dom";
import AppLayout from "./AppLayout";
import CalendarPage from "../pages/CalendarPage";
import MyRequestsPage from "../pages/MyRequestsPage";
import MySeriesRequestsPage from "../pages/MySeriesRequestsPage";
import AdminRequestsPage from "../pages/AdminRequestsPage";
import AdminSeriesRequestsPage from "../pages/AdminSeriesRequestsPage";
import AdminBlockedTimesPage from "../pages/AdminBlockedTimesPage";
import AdminStatisticsPage from "../pages/AdminStatisticsPage";
import AdminUsersPage from "../pages/AdminUsersPage";
import LoginPage from "../features/auth/LoginPage";
import ForgotPasswordPage from "../features/auth/ForgotPasswordPage";
import ResetPasswordPage from "../features/auth/ResetPasswordPage";
import RequireAuth from "../shared/lib/RequireAuth";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <CalendarPage /> },
      { path: "login", element: <LoginPage /> },
      { path: "forgot-password", element: <ForgotPasswordPage /> },
      { path: "reset-password", element: <ResetPasswordPage /> },
      {
        path: "my-requests",
        element: <RequireAuth><MyRequestsPage /></RequireAuth>,
      },
      {
        path: "my-series-requests",
        element: <RequireAuth><MySeriesRequestsPage /></RequireAuth>,
      },
      {
        path: "admin/requests",
        element: <RequireAuth role="ADMIN"><AdminRequestsPage /></RequireAuth>,
      },
      {
        path: "admin/series-requests",
        element: <RequireAuth role="ADMIN"><AdminSeriesRequestsPage /></RequireAuth>,
      },
      {
        path: "admin/blocked-times",
        element: <RequireAuth role="ADMIN"><AdminBlockedTimesPage /></RequireAuth>,
      },
      {
        path: "admin/statistics",
        element: <RequireAuth role="ADMIN"><AdminStatisticsPage /></RequireAuth>,
      },
      {
        path: "admin/users",
        element: <RequireAuth role="ADMIN"><AdminUsersPage /></RequireAuth>,
      },
    ],
  },
]);
