import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router";
import { QueryClientProvider } from "@tanstack/react-query";
import "@fontsource/inter/400.css";
import "@fontsource/inter/600.css";
import "@fontsource/playfair-display/600.css";
import { App } from "./app/App";
import { queryClient } from "./app/query";
import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/shell.css";
import "./styles/homeowner.css";
import "./styles/project-workspace.css";
import "./styles/public-home.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
