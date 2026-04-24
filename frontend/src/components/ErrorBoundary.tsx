import { Component, ErrorInfo, ReactNode } from "react";

type Props = { children: ReactNode };

type State = { hasError: boolean; message: string };

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: "" };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message || "Erro inesperado." };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("ErrorBoundary:", error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="error-boundary-react">
          <div className="error-boundary-react__card">
            <h1 className="error-boundary-react__title">Algo deu errado</h1>
            <p className="error-boundary-react__msg">{this.state.message}</p>
            <button type="button" className="btn-primary-react" onClick={() => window.location.reload()}>
              Recarregar página
            </button>
          </div>
        </main>
      );
    }
    return this.props.children;
  }
}
