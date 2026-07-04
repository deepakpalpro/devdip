import type { ReactNode } from 'react';

interface AppShellProps {
  title: string;
  subtitle?: string;
  nav?: ReactNode;
  theme?: 'consumer' | 'admin';
  children: ReactNode;
}

export function AppShell({ title, subtitle, nav, theme = 'consumer', children }: AppShellProps) {
  return (
    <div className="bf-app-shell" data-theme={theme === 'admin' ? 'admin' : undefined}>
      <header className="bf-app-header">
        <div className="bf-brand">
          <span className="bf-brand-title">{title}</span>
          {subtitle ? <span className="bf-brand-subtitle">{subtitle}</span> : null}
        </div>
        {nav}
      </header>
      <main className="bf-app-main">{children}</main>
    </div>
  );
}

export function PageHeader({ title, description }: { title: string; description?: string }) {
  return (
    <div className="bf-page-header">
      <h1>{title}</h1>
      {description ? <p>{description}</p> : null}
    </div>
  );
}

export function Button({
  children,
  variant = 'primary',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' }) {
  return (
    <button className={`bf-button bf-button-${variant}`} {...props}>
      {children}
    </button>
  );
}

export function Card({
  title,
  meta,
  badge,
  action,
}: {
  title: string;
  meta?: string;
  badge?: string;
  action?: ReactNode;
}) {
  return (
    <article className="bf-card">
      {badge ? <div style={{ marginBottom: '0.75rem' }}><span className="bf-badge">{badge}</span></div> : null}
      <h2 className="bf-card-title">{title}</h2>
      {meta ? <p className="bf-card-meta">{meta}</p> : null}
      {action}
    </article>
  );
}

export function EmptyState({ message }: { message: string }) {
  return <div className="bf-empty-state">{message}</div>;
}

export function LoadingState({ message = 'Loading…' }: { message?: string }) {
  return <div className="bf-loading-state">{message}</div>;
}

export function ErrorState({ message }: { message: string }) {
  return <div className="bf-error-state">{message}</div>;
}
