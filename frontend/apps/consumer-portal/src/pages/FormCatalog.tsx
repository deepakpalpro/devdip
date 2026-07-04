import { Link } from 'react-router-dom';
import { Button, Card, EmptyState, ErrorState, LoadingState } from '@banking-forms/ui';
import { useConsumerForms } from '../hooks/useConsumerForms';

export function FormCatalog() {
  const { data, isLoading, error } = useConsumerForms();

  if (isLoading) {
    return <LoadingState message="Loading available applications…" />;
  }

  if (error) {
    return <ErrorState message={error instanceof Error ? error.message : 'Failed to load forms'} />;
  }

  if (!data?.length) {
    return <EmptyState message="No applications are available right now." />;
  }

  return (
    <div className="bf-card-grid">
      {data.map((form) => (
        <Card
          key={form.code}
          title={form.name}
          meta={form.category ?? 'General'}
          badge={form.code}
          action={
            <Link to={`/apply/${form.code}`}>
              <Button>Start application</Button>
            </Link>
          }
        />
      ))}
    </div>
  );
}
