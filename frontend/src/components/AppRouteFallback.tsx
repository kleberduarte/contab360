/** Skeleton exibido enquanto `React.lazy` carrega o chunk da rota. */
export function AppRouteFallback() {
  return (
    <div className="route-fallback-react" role="status" aria-live="polite" aria-busy="true">
      <div className="route-fallback-react__bar" />
      <div className="route-fallback-react__grid">
        <div className="route-fallback-react__sk route-fallback-react__sk--hero" />
        <div className="route-fallback-react__sk route-fallback-react__sk--line" />
        <div className="route-fallback-react__sk route-fallback-react__sk--line short" />
        <div className="route-fallback-react__row">
          <div className="route-fallback-react__sk route-fallback-react__sk--card" />
          <div className="route-fallback-react__sk route-fallback-react__sk--card" />
          <div className="route-fallback-react__sk route-fallback-react__sk--card" />
        </div>
      </div>
      <span className="route-fallback-react__sr">Carregando tela…</span>
    </div>
  );
}
