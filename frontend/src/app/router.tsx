import { createBrowserRouter } from "react-router-dom";
import AppLayout from "./AppLayout";
import CalendarPage from "../pages/CalendarPage";
import LoginPage from "../features/auth/LoginPage";
import ForgotPasswordPage from "../features/auth/ForgotPasswordPage";
import ResetPasswordPage from "../features/auth/ResetPasswordPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <CalendarPage /> },
      { path: "login", element: <LoginPage /> },
      { path: "forgot-password", element: <ForgotPasswordPage /> },
      { path: "reset-password", element: <ResetPasswordPage /> },
      // TODO: add admin and club-rep routes once those pages are implemented
    ],
  },
]);
