import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";

import './index.css'
import { AuthProvider } from "@/features/auth/auth-context";
import { appRouter } from "@/router/app-router";

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={appRouter} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
